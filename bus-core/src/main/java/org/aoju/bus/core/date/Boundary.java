/*********************************************************************************
 *                                                                               *
 * The MIT License                                                               *
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
 ********************************************************************************/
package org.aoju.bus.core.date;

import org.aoju.bus.core.lang.Fields;
import org.aoju.bus.core.lang.Range;
import org.aoju.bus.core.utils.DateUtils;

import java.util.Date;

/**
 * 日期范围
 *
 * @author Kimi Liu
 * @version 5.9.0
 * @since JDK 1.8+
 */
public class Boundary extends Range<DateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * 构造，包含开始和结束日期时间
     *
     * @param start 起始日期时间
     * @param end   结束日期时间
     * @param unit  步进单位
     */
    public Boundary(Date start, Date end, final Fields.DateField unit) {
        this(start, end, unit, 1);
    }

    /**
     * 构造，包含开始和结束日期时间
     *
     * @param start 起始日期时间
     * @param end   结束日期时间
     * @param unit  步进单位
     * @param step  步进数
     */
    public Boundary(Date start, Date end, final Fields.DateField unit, final int step) {
        this(start, end, unit, step, true, true);
    }

    /**
     * 构造
     *
     * @param start          起始日期时间
     * @param end            结束日期时间
     * @param unit           步进单位
     * @param step           步进数
     * @param isIncludeStart 是否包含开始的时间
     * @param isIncludeEnd   是否包含结束的时间
     */
    public Boundary(Date start, Date end, final Fields.DateField unit, final int step, boolean isIncludeStart, boolean isIncludeEnd) {
        super(DateUtils.date(start), DateUtils.date(end), (current, end1, index) -> {
            DateTime dt = current.offset(unit, step);
            if (dt.isAfter(end1)) {
                return null;
            }
            return current.offset(unit, step);
        }, isIncludeStart, isIncludeEnd);
    }

}
