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
package org.aoju.bus.core.bloom.bitmap;

import java.io.Serializable;

/**
 * 过滤器BitMap在64位机器上.这个类能发生更好的效果.一般机器不建议使用
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public class LongMap implements BitMap, Serializable {

    private static final long serialVersionUID = 1L;

    private final long[] longs;

    /**
     * 构造
     */
    public LongMap() {
        longs = new long[93750000];
    }

    /**
     * 构造
     *
     * @param size 容量
     */
    public LongMap(int size) {
        longs = new long[size];
    }

    @Override
    public void add(long i) {
        int r = (int) (i / BitMap.MACHINE64);
        long c = i % BitMap.MACHINE64;
        longs[r] = longs[r] | (1L << c);
    }

    @Override
    public boolean contains(long i) {
        int r = (int) (i / BitMap.MACHINE64);
        long c = i % BitMap.MACHINE64;
        return ((longs[r] >>> c) & 1) == 1;
    }

    @Override
    public void remove(long i) {
        int r = (int) (i / BitMap.MACHINE64);
        long c = i % BitMap.MACHINE64;
        longs[r] &= ~(1L << c);
    }

}