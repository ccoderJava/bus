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
package org.aoju.bus.core.beans;

import org.aoju.bus.core.convert.Convert;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.toolkit.*;

import java.io.Serializable;
import java.util.*;

/**
 * Bean路径表达式,用于获取多层嵌套Bean中的字段值或Bean对象
 * 根据给定的表达式,查找Bean中对应的属性值对象  表达式分为两种：
 * <ol>
 * <li>.表达式,可以获取Bean对象中的属性(字段)值或者Map中key对应的值</li>
 * <li>[]表达式,可以获取集合等对象中对应index的值</li>
 * </ol>
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public class PathExpression implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表达式边界符号数组
     */
    private static final char[] EXP_CHARS = {Symbol.C_DOT, Symbol.C_BRACKET_LEFT, Symbol.C_BRACKET_RIGHT};
    protected List<String> patternParts;
    private boolean isStartWith = false;

    /**
     * 构造
     *
     * @param expression 表达式
     */
    public PathExpression(String expression) {
        init(expression);
    }

    /**
     * 解析Bean路径表达式为Bean模式
     * Bean表达式,用于获取多层嵌套Bean中的字段值或Bean对象
     * 根据给定的表达式,查找Bean中对应的属性值对象  表达式分为两种：
     * <ol>
     * <li>.表达式,可以获取Bean对象中的属性(字段)值或者Map中key对应的值</li>
     * <li>[]表达式,可以获取集合等对象中对应index的值</li>
     * </ol>
     * <p>
     * 表达式栗子：
     *
     * <pre>
     * persion
     * persion.name
     * persons[3]
     * person.friends[5].name
     * ['person']['friends'][5]['name']
     * </pre>
     *
     * @param expression 表达式
     * @return {@link PathExpression}
     */
    public static PathExpression create(String expression) {
        return new PathExpression(expression);
    }

    private static Object getFieldValue(Object bean, String expression) {
        if (StringKit.isBlank(expression)) {
            return null;
        }

        if (StringKit.contains(expression, Symbol.C_COLON)) {
            // [start:end:step] 模式
            final List<String> parts = StringKit.splitTrim(expression, Symbol.C_COLON);
            int start = Integer.parseInt(parts.get(0));
            int end = Integer.parseInt(parts.get(1));
            int step = 1;
            if (3 == parts.size()) {
                step = Integer.parseInt(parts.get(2));
            }
            if (bean instanceof Collection) {
                return CollKit.sub((Collection<?>) bean, start, end, step);
            } else if (ArrayKit.isArray(bean)) {
                return ArrayKit.sub(bean, start, end, step);
            }
        } else if (StringKit.contains(expression, Symbol.C_COMMA)) {
            final List<String> keys = StringKit.splitTrim(expression, Symbol.C_COMMA);
            if (bean instanceof Collection) {
                return CollKit.getAny((Collection<?>) bean, Convert.convert(int[].class, keys));
            } else if (ArrayKit.isArray(bean)) {
                return ArrayKit.get(bean, Convert.convert(int[].class, keys));
            } else {
                final String[] unWrappedKeys = new String[keys.size()];
                for (int i = 0; i < unWrappedKeys.length; i++) {
                    unWrappedKeys[i] = StringKit.unWrap(keys.get(i), Symbol.C_SINGLE_QUOTE);
                }
                if (bean instanceof Map) {
                    // 只支持String为key的Map
                    MapKit.getAny((Map<String, ?>) bean, unWrappedKeys);
                } else {
                    final Map<String, Object> map = BeanKit.beanToMap(bean);
                    MapKit.getAny(map, unWrappedKeys);
                }
            }
        } else {
            // 数字或普通字符串
            return BeanKit.getFieldValue(bean, expression);
        }

        return null;
    }

    /**
     * 对于非表达式去除单引号
     *
     * @param expression 表达式
     * @return 表达式
     */
    private static String unWrapIfPossible(CharSequence expression) {
        if (StringKit.containsAny(expression, " = ", " > ", " < ", " like ", Symbol.COMMA)) {
            return expression.toString();
        }
        return StringKit.unWrap(expression, Symbol.C_SINGLE_QUOTE);
    }

    /**
     * 获取Bean中对应表达式的值
     *
     * @param bean Bean对象或Map或List等
     * @return 值, 如果对应值不存在, 则返回null
     */
    public Object get(Object bean) {
        return get(this.patternParts, bean, false);
    }

    /**
     * 设置表达式指定位置(或filed对应)的值
     * 若表达式指向一个List则设置其坐标对应位置的值,若指向Map则put对应key的值,Bean则设置字段的值
     * 注意：
     *
     * <pre>
     * 1. 如果为List,如果下标不大于List长度,则替换原有值,否则追加值
     * 2. 如果为数组,如果下标不大于数组长度,则替换原有值,否则追加值
     * </pre>
     *
     * @param bean  Bean、Map或List
     * @param value 值
     */
    public void set(Object bean, Object value) {
        set(bean, this.patternParts, value);
    }

    /**
     * 设置表达式指定位置(或filed对应)的值
     * 若表达式指向一个List则设置其坐标对应位置的值,若指向Map则put对应key的值,Bean则设置字段的值
     * 注意：
     *
     * <pre>
     * 1. 如果为List,如果下标不大于List长度,则替换原有值,否则追加值
     * 2. 如果为数组,如果下标不大于数组长度,则替换原有值,否则追加值
     * </pre>
     *
     * @param bean         Bean、Map或List
     * @param patternParts 表达式块列表
     * @param value        值
     */
    private void set(Object bean, List<String> patternParts, Object value) {
        Object subBean = get(patternParts, bean, true);
        if (null == subBean) {
            set(bean, patternParts.subList(0, patternParts.size() - 1), new HashMap<>());
            //set中有可能做过转换,因此此处重新获取bean
            subBean = get(patternParts, bean, true);
        }
        BeanKit.setFieldValue(subBean, patternParts.get(patternParts.size() - 1), value);
    }

    /**
     * 获取Bean中对应表达式的值
     *
     * @param patternParts 表达式分段列表
     * @param bean         Bean对象或Map或List等
     * @param ignoreLast   是否忽略最后一个值,忽略最后一个值则用于set,否则用于read
     * @return 值, 如果对应值不存在, 则返回null
     */
    private Object get(List<String> patternParts, Object bean, boolean ignoreLast) {
        int length = patternParts.size();
        if (ignoreLast) {
            length--;
        }
        Object subBean = bean;
        boolean isFirst = true;
        String patternPart;
        for (int i = 0; i < length; i++) {
            patternPart = patternParts.get(i);
            subBean = getFieldValue(subBean, patternPart);
            if (null == subBean) {
                // 支持表达式的第一个对象为Bean本身(若用户定义表达式$开头,则不做此操作)
                if (isFirst && false == this.isStartWith && BeanKit.isMatchName(bean, patternPart, true)) {
                    subBean = bean;
                    isFirst = false;
                } else {
                    return null;
                }
            }
        }
        return subBean;
    }

    /**
     * 初始化
     *
     * @param expression 表达式
     */
    private void init(String expression) {
        List<String> localPatternParts = new ArrayList<>();
        int length = expression.length();

        final TextKit builder = new TextKit();
        char c;
        boolean isNumStart = false;// 下标标识符开始
        for (int i = 0; i < length; i++) {
            c = expression.charAt(i);
            if (0 == i && Symbol.C_DOLLAR == c) {
                // 忽略开头的$符,表示当前对象
                isStartWith = true;
                continue;
            }

            if (ArrayKit.contains(EXP_CHARS, c)) {
                // 处理边界符号
                if (Symbol.C_BRACKET_RIGHT == c) {
                    // 中括号(数字下标)结束
                    if (false == isNumStart) {
                        throw new IllegalArgumentException(StringKit.format("Bad expression '{}':{}, we find ']' but no '[' !", expression, i));
                    }
                    isNumStart = false;
                } else {
                    if (isNumStart) {
                        // 非结束中括号情况下发现起始中括号报错(中括号未关闭)
                        throw new IllegalArgumentException(StringKit.format("Bad expression '{}':{}, we find '[' but no ']' !", expression, i));
                    } else if (Symbol.C_BRACKET_LEFT == c) {
                        // 数字下标开始
                        isNumStart = true;
                    }
                }
                // 每一个边界符之前的表达式是一个完整的KEY,开始处理KEY
                if (builder.length() > 0) {
                    localPatternParts.add(unWrapIfPossible(builder));
                }
                builder.reset();
            } else {
                // 非边界符号,追加字符
                builder.append(c);
            }
        }

        // 末尾边界符检查
        if (isNumStart) {
            throw new IllegalArgumentException(StringKit.format("Bad expression '{}':{}, we find '[' but no ']' !", expression, length - 1));
        } else {
            if (builder.length() > 0) {
                localPatternParts.add(unWrapIfPossible(builder));
            }
        }

        // 不可变List
        this.patternParts = Collections.unmodifiableList(localPatternParts);
    }

}
