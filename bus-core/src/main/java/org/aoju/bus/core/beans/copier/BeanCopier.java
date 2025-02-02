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
package org.aoju.bus.core.beans.copier;

import org.aoju.bus.core.beans.copier.provider.BeanValueProvider;
import org.aoju.bus.core.beans.copier.provider.MapValueProvider;
import org.aoju.bus.core.lang.copier.Copier;
import org.aoju.bus.core.lang.exception.InstrumentException;
import org.aoju.bus.core.toolkit.*;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;

/**
 * Bean拷贝
 *
 * @param <T> 目标对象类型
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public class BeanCopier<T> implements Copier<T>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源对象
     */
    private final Object source;
    /**
     * 目标对象
     */
    private final T dest;
    /**
     * 目标的类型(用于泛型类注入)
     */
    private final Type destType;
    /**
     * 拷贝选项
     */
    private final CopyOptions copyOptions;

    /**
     * 构造
     *
     * @param source      来源对象，可以是Bean或者Map
     * @param dest        目标Bean对象
     * @param destType    目标的泛型类型，用于标注有泛型参数的Bean对象
     * @param copyOptions 拷贝属性选项
     */
    public BeanCopier(Object source, T dest, Type destType, CopyOptions copyOptions) {
        this.source = source;
        this.dest = dest;
        this.destType = destType;
        this.copyOptions = copyOptions;
    }

    /**
     * 创建BeanCopier
     *
     * @param <T>         目标Bean类型
     * @param source      来源对象，可以是Bean或者Map
     * @param dest        目标Bean对象
     * @param copyOptions 拷贝属性选项
     * @return BeanCopier
     */
    public static <T> BeanCopier<T> create(Object source, T dest, CopyOptions copyOptions) {
        return create(source, dest, dest.getClass(), copyOptions);
    }

    /**
     * 创建BeanCopier
     *
     * @param <T>         目标Bean类型
     * @param source      来源对象,可以是Bean或者Map
     * @param dest        目标Bean对象
     * @param destType    目标的泛型类型,用于标注有泛型参数的Bean对象
     * @param copyOptions 拷贝属性选项
     * @return BeanCopier
     */
    public static <T> BeanCopier<T> create(Object source, T dest, Type destType, CopyOptions copyOptions) {
        return new BeanCopier<>(source, dest, destType, copyOptions);
    }

    /**
     * 获取指定字段名对应的映射值
     *
     * @param mapping   反向映射Map
     * @param fieldName 字段名
     * @return 映射值，无对应值返回字段名
     */
    private static String mappingKey(Map<String, String> mapping, String fieldName) {
        if (MapKit.isEmpty(mapping)) {
            return fieldName;
        }
        return ObjectKit.defaultIfNull(mapping.get(fieldName), fieldName);
    }

    @Override
    public T copy() {
        if (null != this.source) {
            if (this.source instanceof ValueProvider) {
                // 目标只支持Bean
                valueProviderToBean((ValueProvider<String>) this.source, this.dest);
            } else if (this.source instanceof Map) {
                if (this.dest instanceof Map) {
                    mapToMap((Map<?, ?>) this.source, (Map<?, ?>) this.dest);
                } else {
                    mapToBean((Map<?, ?>) this.source, this.dest);
                }
            } else {
                if (this.dest instanceof Map) {
                    beanToMap(this.source, (Map<?, ?>) this.dest);
                } else {
                    beanToBean(this.source, this.dest);
                }
            }
        }
        return this.dest;
    }

    /**
     * Bean和Bean之间属性拷贝
     *
     * @param providerBean 来源Bean
     * @param destBean     目标Bean
     */
    private void beanToBean(Object providerBean, Object destBean) {
        valueProviderToBean(new BeanValueProvider(providerBean, this.copyOptions.ignoreCase, this.copyOptions.ignoreError), destBean);
    }

    /**
     * Map转Bean属性拷贝
     *
     * @param map  Map
     * @param bean Bean
     */
    private void mapToBean(Map<?, ?> map, Object bean) {
        valueProviderToBean(
                new MapValueProvider(map, this.copyOptions.ignoreCase, this.copyOptions.ignoreError),
                bean
        );
    }

    /**
     * Map转Map
     *
     * @param source 源Map
     * @param dest   目标Map
     */
    private void mapToMap(Map source, Map dest) {
        source.forEach((key, value) -> {
            final CopyOptions copyOptions = this.copyOptions;
            final HashSet<String> ignoreSet = (null != copyOptions.ignoreProperties) ? CollKit.newHashSet(copyOptions.ignoreProperties) : null;

            // 非覆盖模式下，如果目标值存在，则跳过
            if (false == copyOptions.override && null != dest.get(key)) {
                return;
            }

            if (key instanceof CharSequence) {
                if (CollKit.contains(ignoreSet, key)) {
                    // 目标属性值被忽略或值提供者无此key时跳过
                    return;
                }

                // 对key做映射，映射后为null的忽略之
                key = copyOptions.editFieldName(copyOptions.getMappedFieldName(key.toString(), false));
                if (null == key) {
                    return;
                }

                value = copyOptions.editFieldValue(key.toString(), value);
            }

            if ((null == value && copyOptions.ignoreNullValue) || source == value) {
                // 当允许跳过空时，跳过
                //值不能为bean本身，防止循环引用，此类也跳过
                return;
            }

            dest.put(key, value);
        });
    }

    /**
     * 对象转Map
     *
     * @param bean      bean对象
     * @param targetMap 目标的Map
     */
    private void beanToMap(Object bean, Map targetMap) {
        final CopyOptions copyOptions = this.copyOptions;
        final HashSet<String> ignoreSet = (null != copyOptions.ignoreProperties) ? CollKit.newHashSet(copyOptions.ignoreProperties) : null;

        BeanKit.descForEach(bean.getClass(), (prop) -> {
            if (false == prop.isReadable(copyOptions.transientSupport)) {
                // 忽略的属性跳过之
                return;
            }
            String key = prop.getFieldName();
            if (CollKit.contains(ignoreSet, key)) {
                // 目标属性值被忽略或值提供者无此key时跳过
                return;
            }

            // 对key做映射，映射后为null的忽略之
            key = copyOptions.editFieldName(copyOptions.getMappedFieldName(key, false));
            if (null == key) {
                return;
            }

            // 非覆盖模式下，如果目标值存在，则跳过
            if (false == copyOptions.override && null != targetMap.get(key)) {
                return;
            }

            Object value;
            try {
                value = prop.getValue(bean);
            } catch (Exception e) {
                if (copyOptions.ignoreError) {
                    return;// 忽略反射失败
                } else {
                    throw new InstrumentException("Get value of [{}] error!", prop.getFieldName());
                }
            }
            if (null != copyOptions.propertiesFilter && false == copyOptions.propertiesFilter.test(prop.getField(), value)) {
                return;
            }

            value = copyOptions.editFieldValue(key, value);

            if ((null == value && copyOptions.ignoreNullValue) || bean == value) {
                // 当允许跳过空时，跳过
                //值不能为bean本身，防止循环引用，此类也跳过
                return;
            }

            targetMap.put(key, value);
        });
    }

    /**
     * 值提供器转Bean
     *
     * @param valueProvider 值提供器
     * @param bean          Bean
     */
    private void valueProviderToBean(ValueProvider<String> valueProvider, Object bean) {
        if (null == valueProvider) {
            return;
        }

        final CopyOptions copyOptions = this.copyOptions;
        Class<?> actualEditable = bean.getClass();
        if (null != copyOptions.editable) {
            // 检查限制类是否为target的父类或接口
            if (false == copyOptions.editable.isInstance(bean)) {
                throw new IllegalArgumentException(StringKit.format("Target class [{}] not assignable to Editable class [{}]", bean.getClass().getName(), copyOptions.editable.getName()));
            }
            actualEditable = copyOptions.editable;
        }
        final HashSet<String> ignoreSet = (null != copyOptions.ignoreProperties) ? CollKit.newHashSet(copyOptions.ignoreProperties) : null;

        // 遍历目标bean的所有属性
        BeanKit.descForEach(actualEditable, (prop) -> {
            if (false == prop.isWritable(this.copyOptions.transientSupport)) {
                // 字段不可写，跳过之
                return;
            }
            // 检查属性名
            String fieldName = prop.getFieldName();
            if (CollKit.contains(ignoreSet, fieldName)) {
                // 目标属性值被忽略或值提供者无此key时跳过
                return;
            }

            // 对key做映射，映射后为null的忽略之
            // 这里 copyOptions.editFieldName() 不能少，否则导致 CopyOptions setFieldNameEditor 失效
            final String providerKey = copyOptions.editFieldName(copyOptions.getMappedFieldName(fieldName, true));
            if (null == providerKey) {
                return;
            }
            if (false == valueProvider.containsKey(providerKey)) {
                // 无对应值可提供
                return;
            }

            // 获取目标字段真实类型
            final Type fieldType = TypeKit.getActualType(this.destType, prop.getFieldType());

            // 获取属性值
            Object value = valueProvider.value(providerKey, fieldType);
            if (null != copyOptions.propertiesFilter && false == copyOptions.propertiesFilter.test(prop.getField(), value)) {
                return;
            }

            value = copyOptions.editFieldValue(providerKey, value);

            if ((null == value && copyOptions.ignoreNullValue) || bean == value) {
                // 当允许跳过空时，跳过
                // 值不能为bean本身，防止循环引用
                return;
            }

            prop.setValue(bean, value, copyOptions.ignoreNullValue, copyOptions.ignoreError, copyOptions.override);
        });
    }

}
