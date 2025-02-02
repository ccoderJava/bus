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

import org.aoju.bus.core.lang.Editor;
import org.aoju.bus.core.toolkit.MapKit;
import org.aoju.bus.core.toolkit.ObjectKit;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * 属性拷贝选项
 * 包括：
 * 1、限制的类或接口,必须为目标对象的实现接口或父类,用于限制拷贝的属性,
 * 例如一个类我只想复制其父类的一些属性,就可以将editable设置为父类
 * 2、是否忽略空值,当源对象的值为null时,true: 忽略,false: 注入
 * 3、忽略的属性列表,设置一个属性列表,不拷贝这些属性值
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public class CopyOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 限制的类或接口,必须为目标对象的实现接口或父类,用于限制拷贝的属性,例如一个类我只想复制其父类的一些属性,就可以将editable设置为父类
     */
    protected Class<?> editable;
    /**
     * 是否忽略空值,当源对象的值为null时,true: 忽略而不注入此值,false: 注入null
     */
    protected boolean ignoreNullValue;
    /**
     * 忽略的目标对象中属性列表,设置一个属性列表,不拷贝这些属性值
     */
    protected BiPredicate<Field, Object> propertiesFilter;
    /**
     * 忽略的目标对象中属性列表，设置一个属性列表，不拷贝这些属性值
     */
    protected String[] ignoreProperties;
    /**
     * 是否忽略字段注入错误
     */
    protected boolean ignoreError;
    /**
     * 是否忽略字段大小写
     */
    protected boolean ignoreCase;
    /**
     * 拷贝属性的字段映射,用于不同的属性之前拷贝做对应表用
     */
    protected Map<String, String> fieldMapping;
    /**
     * 字段属性编辑器，用于自定义属性转换规则，例如驼峰转下划线等
     */
    protected Editor<String> fieldNameEditor;
    /**
     * 反向映射表，自动生成用于反向查找
     */
    protected BiFunction<String, Object, Object> fieldValueEditor;
    /**
     * 反向映射表，自动生成用于反向查找
     */
    protected Map<String, String> reversedFieldMapping;
    /**
     * 是否支持transient关键字修饰和@Transient注解，如果支持，被修饰的字段或方法对应的字段将被忽略。
     */
    protected boolean transientSupport = false;
    /**
     * 是否覆盖目标值，如果不覆盖，会先读取目标对象的值，非{@code null}则写，否则忽略。如果覆盖，则不判断直接写
     */
    protected boolean override = true;

    /**
     * 构造拷贝选项
     */
    public CopyOptions() {

    }

    /**
     * 构造拷贝选项
     *
     * @param editable         限制的类或接口，必须为目标对象的实现接口或父类，用于限制拷贝的属性
     * @param ignoreNullValue  是否忽略空值，当源对象的值为null时，true: 忽略而不注入此值，false: 注入null
     * @param ignoreProperties 忽略的目标对象中属性列表，设置一个属性列表，不拷贝这些属性值
     */
    public CopyOptions(Class<?> editable, boolean ignoreNullValue, String... ignoreProperties) {
        this.propertiesFilter = (f, v) -> true;
        this.editable = editable;
        this.ignoreNullValue = ignoreNullValue;
        this.ignoreProperties = ignoreProperties;
    }

    /**
     * 创建拷贝选项
     *
     * @return 拷贝选项
     */
    public static CopyOptions create() {
        return new CopyOptions();
    }

    /**
     * 创建拷贝选项
     *
     * @param editable         限制的类或接口，必须为目标对象的实现接口或父类，用于限制拷贝的属性
     * @param ignoreNullValue  是否忽略空值，当源对象的值为null时，true: 忽略而不注入此值，false: 注入null
     * @param ignoreProperties 忽略的属性列表，设置一个属性列表，不拷贝这些属性值
     * @return 拷贝选项
     */
    public static CopyOptions create(Class<?> editable, boolean ignoreNullValue, String... ignoreProperties) {
        return new CopyOptions(editable, ignoreNullValue, ignoreProperties);
    }

    /**
     * 设置限制的类或接口,必须为目标对象的实现接口或父类,用于限制拷贝的属性
     *
     * @param editable 限制的类或接口
     * @return this
     */
    public CopyOptions setEditable(Class<?> editable) {
        this.editable = editable;
        return this;
    }

    /**
     * 设置是否忽略空值,当源对象的值为null时,true: 忽略而不注入此值,false: 注入null
     *
     * @param ignoreNullVall 是否忽略空值,当源对象的值为null时,true: 忽略而不注入此值,false: 注入null
     * @return this
     */
    public CopyOptions setIgnoreNullValue(boolean ignoreNullVall) {
        this.ignoreNullValue = ignoreNullVall;
        return this;
    }

    /**
     * 设置忽略空值，当源对象的值为null时，忽略而不注入此值
     *
     * @return this
     */
    public CopyOptions ignoreNullValue() {
        return setIgnoreNullValue(true);
    }

    /**
     * 属性过滤器，断言通过的属性才会被复制
     *
     * @param propertiesFilter 属性过滤器
     * @return this
     */
    public CopyOptions setPropertiesFilter(BiPredicate<Field, Object> propertiesFilter) {
        this.propertiesFilter = propertiesFilter;
        return this;
    }

    /**
     * 设置忽略的目标对象中属性列表，设置一个属性列表，不拷贝这些属性值
     *
     * @param ignoreProperties 忽略的目标对象中属性列表，设置一个属性列表，不拷贝这些属性值
     * @return this
     */
    public CopyOptions setIgnoreProperties(String... ignoreProperties) {
        this.ignoreProperties = ignoreProperties;
        return this;
    }

    /**
     * 设置是否忽略字段的注入错误
     *
     * @param ignoreError 是否忽略注入错误
     * @return this
     */
    public CopyOptions setIgnoreError(boolean ignoreError) {
        this.ignoreError = ignoreError;
        return this;
    }

    /**
     * 设置忽略字段的注入错误
     *
     * @return this
     */
    public CopyOptions ignoreError() {
        return setIgnoreError(true);
    }

    /**
     * 设置是否忽略字段的大小写
     *
     * @param ignoreCase 是否忽略大小写
     * @return this
     */
    public CopyOptions setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        return this;
    }

    /**
     * 设置忽略字段的大小写
     *
     * @return this
     */
    public CopyOptions ignoreCase() {
        return setIgnoreCase(true);
    }

    /**
     * 设置拷贝属性的字段映射，用于不同的属性之前拷贝做对应表用
     *
     * @param fieldMapping 拷贝属性的字段映射，用于不同的属性之前拷贝做对应表用
     * @return this
     */
    public CopyOptions setFieldMapping(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping;
        return this;
    }

    /**
     * 设置字段属性编辑器，用于自定义属性转换规则，例如驼峰转下划线等
     * 此转换器只针对源端的字段做转换，请确认转换后与目标端字段一致
     * 当转换后的字段名为null时忽略这个字段
     *
     * @param fieldNameEditor 字段属性编辑器，用于自定义属性转换规则，例如驼峰转下划线等
     * @return this
     */
    public CopyOptions setFieldNameEditor(Editor<String> fieldNameEditor) {
        this.fieldNameEditor = fieldNameEditor;
        return this;
    }

    /**
     * 设置字段属性值编辑器，用于自定义属性值转换规则，例如null转""等
     *
     * @param fieldValueEditor 字段属性值编辑器，用于自定义属性值转换规则，例如null转""等
     * @return this
     */
    public CopyOptions setFieldValueEditor(BiFunction<String, Object, Object> fieldValueEditor) {
        this.fieldValueEditor = fieldValueEditor;
        return this;
    }

    /**
     * 编辑字段值
     *
     * @param fieldName  字段名
     * @param fieldValue 字段值
     * @return 编辑后的字段值
     */
    protected Object editFieldValue(String fieldName, Object fieldValue) {
        return (null != this.fieldValueEditor) ?
                this.fieldValueEditor.apply(fieldName, fieldValue) : fieldValue;
    }

    /**
     * 设置是否支持transient关键字修饰和@Transient注解，如果支持，被修饰的字段或方法对应的字段将被忽略。
     *
     * @param transientSupport 是否支持
     * @return this
     */
    public CopyOptions setTransientSupport(boolean transientSupport) {
        this.transientSupport = transientSupport;
        return this;
    }

    /**
     * 设置是否覆盖目标值，如果不覆盖，会先读取目标对象的值，非{@code null}则写，否则忽略。如果覆盖，则不判断直接写
     *
     * @param override 是否覆盖目标值
     * @return this
     */
    public CopyOptions setOverride(boolean override) {
        this.override = override;
        return this;
    }

    /**
     * 获得映射后的字段名
     * 当非反向，则根据源字段名获取目标字段名，反之根据目标字段名获取源字段名。
     *
     * @param fieldName 字段名
     * @param reversed  是否反向映射
     * @return 映射后的字段名
     */
    protected String getMappedFieldName(String fieldName, boolean reversed) {
        Map<String, String> mapping = reversed ? getReversedMapping() : this.fieldMapping;
        if (MapKit.isEmpty(mapping)) {
            return fieldName;
        }
        return ObjectKit.defaultIfNull(mapping.get(fieldName), fieldName);
    }

    /**
     * 编辑字段值
     *
     * @param fieldName 字段名
     * @return 编辑后的字段名
     */
    protected String editFieldName(String fieldName) {
        return (null != this.fieldNameEditor) ? this.fieldNameEditor.edit(fieldName) : fieldName;
    }

    /**
     * 获取反转之后的映射
     *
     * @return 反转映射
     */
    private Map<String, String> getReversedMapping() {
        if (null == this.fieldMapping) {
            return null;
        }
        if (null == this.reversedFieldMapping) {
            reversedFieldMapping = MapKit.reverse(this.fieldMapping);
        }
        return reversedFieldMapping;
    }

}
