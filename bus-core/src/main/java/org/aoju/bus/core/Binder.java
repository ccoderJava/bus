/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 *                                                                               *
 ********************************************************************************/
package org.aoju.bus.core;

import lombok.RequiredArgsConstructor;
import org.aoju.bus.core.annotation.Ignore;
import org.aoju.bus.core.annotation.Values;
import org.aoju.bus.core.convert.Convert;
import org.aoju.bus.core.io.resource.PropertySource;
import org.aoju.bus.core.lang.Assert;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.lang.exception.InstrumentException;
import org.aoju.bus.core.toolkit.ClassKit;
import org.aoju.bus.core.toolkit.StringKit;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 属性绑定器
 *
 * @author Kimi Liu
 * @version 6.2.0
 * @since JDK 1.8+
 */
@RequiredArgsConstructor
public class Binder {

    /**
     * 默认占位符前缀: {@value}
     */
    public static final String DEFAULT_PLACEHOLDER_PREFIX = Symbol.DOLLAR + Symbol.BRACE_LEFT;

    /**
     * 默认占位符后缀: {@value}
     */
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = Symbol.BRACE_RIGHT;

    /**
     * 默认占位值分隔符: {@value}
     */
    public static final String DEFAULT_VALUE_SEPARATOR = Symbol.COLON;

    /**
     * 默认实例
     */
    public static final Binder DEFAULT_HELPER;

    private static final Map<String, String> SIMPLE_PREFIXES = new HashMap<>(4);

    static {
        SIMPLE_PREFIXES.put(Symbol.BRACE_RIGHT, Symbol.BRACE_LEFT);
        SIMPLE_PREFIXES.put(Symbol.BRACKET_RIGHT, Symbol.BRACKET_LEFT);
        SIMPLE_PREFIXES.put(Symbol.PARENTHESE_RIGHT, Symbol.PARENTHESE_LEFT);

        DEFAULT_HELPER = new Binder(DEFAULT_PLACEHOLDER_PREFIX, DEFAULT_PLACEHOLDER_SUFFIX,
                DEFAULT_VALUE_SEPARATOR, true);
    }

    private final String placeholderPrefix;
    private final String placeholderSuffix;
    private final String simplePrefix;
    private final String valueSeparator;
    private final boolean ignoreUnresolvablePlaceholders;
    private PropertySource source;

    public Binder(String placeholderPrefix, String placeholderSuffix) {
        this(placeholderPrefix, placeholderSuffix, null, true);
    }

    public Binder(String placeholderPrefix,
                  String placeholderSuffix,
                  String valueSeparator,
                  boolean ignoreUnresolvablePlaceholders) {

        Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
        Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        String simplePrefixForSuffix = SIMPLE_PREFIXES.get(this.placeholderSuffix);
        if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
            this.simplePrefix = simplePrefixForSuffix;
        } else {
            this.simplePrefix = this.placeholderPrefix;
        }
        this.valueSeparator = valueSeparator;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    private static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        if (index + substring.length() > str.length()) {
            return false;
        }
        for (int i = 0; i < substring.length(); i++) {
            if (str.charAt(index + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 绑定属性到类
     *
     * @param clazz 类型
     * @param <T>   泛型
     * @return obj
     */
    public <T> T bind(Class<T> clazz) {
        return bind(clazz, Symbol.DOT);
    }

    /**
     * 绑定指定前缀属性到类
     *
     * @param clazz  类型
     * @param prefix 前缀
     * @param <T>    泛型
     * @return obj
     */
    public <T> T bind(Class<T> clazz, String prefix) {
        T obj;
        try {
            obj = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new InstrumentException(e);
        }

        Class<?> actualClass = ClassKit.getCglibActualClass(clazz);
        boolean b = (prefix == null || Symbol.DOT.equals(prefix)) && actualClass.isAnnotationPresent(Values.class);
        if (b) {
            prefix = actualClass.getAnnotation(Values.class).value();
        }
        return clazz.cast(bind(obj, prefix));
    }

    /**
     * 绑定属性到对象实例
     *
     * @param obj    对象实例
     * @param prefix 属性前缀
     * @param <T>    泛型
     * @return obj
     */
    public <T> T bind(T obj, String prefix) {
        if (!StringKit.hasText(prefix) || Symbol.DOT.equals(prefix)) {
            prefix = null;
        }
        for (Field field : ClassKit.getDeclaredFields(obj.getClass())) {
            bindField(obj, field, prefix);
        }
        return obj;
    }

    private void bindField(Object obj, Field field, String prefix) {
        try {
            if (field.isAnnotationPresent(Ignore.class)) {
                return;
            }
            String key = field.getName();
            boolean wrap = false;
            if (field.isAnnotationPresent(Values.class)) {
                key = field.getAnnotation(Values.class).value();
                wrap = true;
            } else if (prefix != null) {
                key = prefix + Symbol.DOT + key;
            }
            Object value = getProperty(key, field.getType(), wrap);
            if (value != null) {
                ClassKit.writeField(field, obj, value);
                return;
            }
            if (!(field.getType().getClassLoader() == null) && source
                    .containPrefix(key + Symbol.DOT)) {
                value = ClassKit.readField(field, obj);
                if (value == null) {
                    value = bind(field.getType(), key);
                    ClassKit.writeField(field, obj, value);
                } else {
                    bind(value, key);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Object getProperty(String key, Class<?> type, boolean wrap) {
        String value;
        if (wrap) {
            value = source.getPlaceholderProperty(key);
        } else {
            value = source.getProperty(key);
        }
        if (value == null || value.getClass() == type) {
            return value;
        }
        return Convert.convert(type, value);
    }

    /**
     * 替换所有占位格式 {@code ${name}}
     *
     * @param value      需要转换的属性
     * @param properties 配置集合
     * @return 替换后的字符串
     */
    public String replacePlaceholders(String value, final Properties properties) {
        Assert.notNull(properties, "'properties' must not be null");
        Assert.notNull(value, "'value' must not be null");
        return parseStringValue(value, properties, new HashSet<>());
    }

    protected String parseStringValue(String value, Properties properties,
                                      Set<String> visitedPlaceholders) {
        StringBuilder result = new StringBuilder(value);
        int startIndex = value.indexOf(this.placeholderPrefix);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(result, startIndex);
            if (endIndex != -1) {
                String placeholder = result
                        .substring(startIndex + this.placeholderPrefix.length(), endIndex);
                String originalPlaceholder = placeholder;
                if (!visitedPlaceholders.add(originalPlaceholder)) {
                    throw new IllegalArgumentException(
                            "Circular placeholder reference '" + originalPlaceholder
                                    + "' in property definitions");
                }
                // 递归
                placeholder = parseStringValue(placeholder, properties, visitedPlaceholders);
                // 获取没有占位的属性值
                String propVal = properties.getProperty(placeholder);
                propVal = getCommonVal(properties, placeholder, propVal);
                if (propVal != null) {
                    // 递归 表达式中含有表达式
                    propVal = parseStringValue(propVal, properties, visitedPlaceholders);
                    result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
                    startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
                } else if (this.ignoreUnresolvablePlaceholders) {
                    // 继续解析
                    startIndex = result
                            .indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
                } else {
                    throw new IllegalArgumentException(String
                            .format("Could not resolve placeholder '%s' in value '%s'", placeholder, value));
                }
                visitedPlaceholders.remove(originalPlaceholder);
            } else {
                startIndex = -1;
            }
        }
        return result.toString();
    }

    private String getCommonVal(Properties properties, String placeholder, String propVal) {
        if (propVal == null && this.valueSeparator != null) {
            int separatorIndex = placeholder.indexOf(this.valueSeparator);
            if (separatorIndex != -1) {
                String actualPlaceholder = placeholder.substring(0, separatorIndex);
                String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
                propVal = properties.getProperty(actualPlaceholder);
                if (propVal == null) {
                    propVal = defaultValue;
                }
            }
        }
        return propVal;
    }

    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, this.placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + this.placeholderSuffix.length();
                } else {
                    return index;
                }
            } else if (substringMatch(buf, index, this.simplePrefix)) {
                withinNestedPlaceholder++;
                index = index + this.simplePrefix.length();
            } else {
                index++;
            }
        }
        return -1;
    }

}
