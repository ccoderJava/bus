/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org and other contributors.                      *
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
package org.aoju.bus.extra.effect;

import org.aoju.bus.core.annotation.SPI;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 解压缩服务工厂
 *
 * @author Kimi Liu
 * @version 6.1.8
 * @since JDK 1.8+
 */
public enum EffectFactory {

    CF;

    Map<String, EffectProvider> compressMap = new HashMap<>();

    EffectFactory() {
        ServiceLoader<EffectProvider> compresses = ServiceLoader.load(EffectProvider.class);
        for (EffectProvider effectProvider : compresses) {
            SPI spi = effectProvider.getClass().getAnnotation(SPI.class);
            if (spi != null) {
                String name = spi.value();
                if (compressMap.containsKey(name)) {
                    throw new RuntimeException("The @SPI value(" + name
                            + ") repeat, for class(" + effectProvider.getClass()
                            + ") and class(" + compressMap.get(name).getClass()
                            + ").");
                }

                compressMap.put(name, effectProvider);
            }
        }
    }

    /**
     * 获取解压缩服务提供者 @SPI 值是{#name} 名称
     *
     * @param name 名称
     * @return 服务提供者
     */
    public EffectProvider get(String name) {
        return compressMap.get(name);
    }

}