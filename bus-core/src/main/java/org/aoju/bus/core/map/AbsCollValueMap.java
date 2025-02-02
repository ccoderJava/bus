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
package org.aoju.bus.core.map;

import org.aoju.bus.core.toolkit.CollKit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 值作为集合的Map实现，通过调用putValue可以在相同key时加入多个值，多个值用集合表示
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @param <C> 集合类型
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public abstract class AbsCollValueMap<K, V, C extends Collection<V>> extends MapWrapper<K, C> {

    /**
     * 默认集合初始大小
     */
    protected static final int DEFAULT_COLLECTION_INITIAL_CAPACITY = 3;

    /**
     * 构造
     */
    public AbsCollValueMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * 构造
     *
     * @param initialCapacity 初始大小
     */
    public AbsCollValueMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 构造
     *
     * @param m Map
     */
    public AbsCollValueMap(Map<? extends K, C> m) {
        this(DEFAULT_LOAD_FACTOR, m);
    }

    /**
     * 构造
     *
     * @param loadFactor 加载因子
     * @param m          Map
     */
    public AbsCollValueMap(float loadFactor, Map<? extends K, C> m) {
        this(m.size(), loadFactor);
        this.putAll(m);
    }

    /**
     * 构造
     *
     * @param initialCapacity 初始大小
     * @param loadFactor      加载因子
     */
    public AbsCollValueMap(int initialCapacity, float loadFactor) {
        super(new HashMap<>(initialCapacity, loadFactor));
    }

    /**
     * 放入所有value
     *
     * @param m valueMap
     */
    public void putAllValues(Map<? extends K, ? extends Collection<V>> m) {
        if (null != m) {
            m.forEach((key, valueColl) -> {
                if (null != valueColl) {
                    valueColl.forEach((value) -> putValue(key, value));
                }
            });
        }
    }

    /**
     * 放入Value
     * 如果键对应值列表有值，加入，否则创建一个新列表后加入
     *
     * @param key   键
     * @param value 值
     */
    public void putValue(K key, V value) {
        C collection = this.get(key);
        if (null == collection) {
            collection = createCollection();
            this.put(key, collection);
        }
        collection.add(value);
    }

    /**
     * 获取值
     *
     * @param key   键
     * @param index 第几个值的索引，越界返回null
     * @return 值或null
     */
    public V get(K key, int index) {
        final Collection<V> collection = get(key);
        return CollKit.get(collection, index);
    }

    /**
     * 创建集合
     * 此方法用于创建在putValue后追加值所在的集合，子类实现此方法创建不同类型的集合
     *
     * @return {@link Collection}
     */
    protected abstract C createCollection();

}
