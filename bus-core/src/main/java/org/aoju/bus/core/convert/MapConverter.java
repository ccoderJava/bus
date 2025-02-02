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
package org.aoju.bus.core.convert;

import org.aoju.bus.core.toolkit.BeanKit;
import org.aoju.bus.core.toolkit.MapKit;
import org.aoju.bus.core.toolkit.StringKit;
import org.aoju.bus.core.toolkit.TypeKit;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * {@link Map} 转换器
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public class MapConverter extends AbstractConverter<Map<?, ?>> {

    private static final long serialVersionUID = 1L;

    /**
     * Map类型
     */
    private final Type mapType;
    /**
     * 键类型
     */
    private final Type keyType;
    /**
     * 值类型
     */
    private final Type valueType;

    /**
     * 构造,Map的key和value泛型类型自动获取
     *
     * @param mapType Map类型
     */
    public MapConverter(Type mapType) {
        this(mapType, TypeKit.getTypeArgument(mapType, 0), TypeKit.getTypeArgument(mapType, 1));
    }

    /**
     * 构造
     *
     * @param mapType   Map类型
     * @param keyType   键类型
     * @param valueType 值类型
     */
    public MapConverter(Type mapType, Type keyType, Type valueType) {
        this.mapType = mapType;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    protected Map<?, ?> convertInternal(Object value) {
        Map map;
        if (value instanceof Map) {
            final Type[] typeArguments = TypeKit.getTypeArguments(value.getClass());
            if (null != typeArguments
                    && 2 == typeArguments.length
                    && Objects.equals(this.keyType, typeArguments[0])
                    && Objects.equals(this.valueType, typeArguments[1])) {
                // 对于键值对类型一致的Map对象，不再做转换，直接返回原对象
                return (Map) value;
            }
            map = MapKit.createMap(TypeKit.getClass(this.mapType));
            convertMapToMap((Map) value, map);
        } else if (BeanKit.isBean(value.getClass())) {
            map = BeanKit.beanToMap(value);
            // 二次转换，转换键值类型
            map = convertInternal(map);
        } else {
            throw new UnsupportedOperationException(StringKit.format("Unsupport toMap value type: {}", value.getClass().getName()));
        }
        return map;
    }

    /**
     * Map转Map
     *
     * @param srcMap    源Map
     * @param targetMap 目标Map
     */
    private void convertMapToMap(Map<?, ?> srcMap, Map<Object, Object> targetMap) {
        final ConverterRegistry convert = ConverterRegistry.getInstance();
        Object key;
        Object value;
        for (Entry<?, ?> entry : srcMap.entrySet()) {
            key = TypeKit.isUnknown(this.keyType) ? entry.getKey() : convert.convert(this.keyType, entry.getKey());
            value = TypeKit.isUnknown(this.valueType) ? entry.getValue() : convert.convert(this.valueType, entry.getValue());
            targetMap.put(key, value);
        }
    }

    @Override
    public Class<Map<?, ?>> getTargetType() {
        return (Class<Map<?, ?>>) TypeKit.getClass(this.mapType);
    }

}
