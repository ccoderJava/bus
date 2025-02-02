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

import org.aoju.bus.core.date.DateTime;
import org.aoju.bus.core.lang.exception.InstrumentException;
import org.aoju.bus.core.toolkit.DateKit;
import org.aoju.bus.core.toolkit.StringKit;

import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期转换器
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public class DateConverter extends AbstractConverter<Date> {

    private static final long serialVersionUID = 1L;

    private final Class<? extends Date> targetType;
    /**
     * 日期格式
     */
    private String format;

    /**
     * 构造
     *
     * @param targetType 目标类型
     */
    public DateConverter(Class<? extends Date> targetType) {
        this.targetType = targetType;
    }

    /**
     * 构造
     *
     * @param targetType 目标类型
     * @param format     日期格式
     */
    public DateConverter(Class<? extends Date> targetType, String format) {
        this.targetType = targetType;
        this.format = format;
    }

    /**
     * 获取日期格式
     *
     * @return 设置日期格式
     */
    public String getFormat() {
        return format;
    }

    /**
     * 设置日期格式
     *
     * @param format 日期格式
     */
    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    protected Date convertInternal(Object value) {
        if (value == null || (value instanceof CharSequence
                && StringKit.isBlank(value.toString()))) {
            return null;
        }
        if (value instanceof TemporalAccessor) {
            return wrap(DateKit.date((TemporalAccessor) value));
        } else if (value instanceof Calendar) {
            return wrap(DateKit.date((Calendar) value));
        } else if (value instanceof Number) {
            return wrap(((Number) value).longValue());
        } else {
            // 统一按照字符串处理
            final String valueStr = convertString(value);
            final DateTime dateTime = StringKit.isBlank(this.format)
                    ? DateKit.parse(valueStr) //
                    : DateKit.parse(valueStr, this.format);
            if (null != dateTime) {
                return wrap(dateTime);
            }
        }
        throw new InstrumentException("Can not convert {}:[{}] to {}", value.getClass().getName(), value, this.targetType.getName());
    }

    @Override
    public Class<java.util.Date> getTargetType() {
        return (Class<java.util.Date>) this.targetType;
    }

    /**
     * java.util.Date转为子类型
     *
     * @param dateTime 时间
     * @return 目标类型对象
     */
    private java.util.Date wrap(DateTime dateTime) {
        // 返回指定类型
        if (java.util.Date.class == targetType) {
            return dateTime;
        }
        if (DateTime.class == targetType) {
            return DateKit.date(dateTime);
        }
        if (java.sql.Date.class == targetType) {
            return dateTime.toSqlDate();
        }
        if (java.sql.Time.class == targetType) {
            return new java.sql.Time(dateTime.getTime());
        }
        if (java.sql.Timestamp.class == targetType) {
            return dateTime.toTimestamp();
        }

        throw new UnsupportedOperationException(StringKit.format("Unsupported target Date type: {}", this.targetType.getName()));
    }

    /**
     * java.util.Date转为子类型
     *
     * @param mills Date
     * @return 目标类型对象
     */
    private java.util.Date wrap(long mills) {
        // 返回指定类型
        if (java.util.Date.class == targetType) {
            return new java.util.Date(mills);
        }
        if (DateTime.class == targetType) {
            return DateKit.date(mills);
        }
        if (java.sql.Date.class == targetType) {
            return new java.sql.Date(mills);
        }
        if (java.sql.Time.class == targetType) {
            return new java.sql.Time(mills);
        }
        if (java.sql.Timestamp.class == targetType) {
            return new java.sql.Timestamp(mills);
        }

        throw new UnsupportedOperationException(StringKit.format("Unsupported target Date type: {}", this.targetType.getName()));
    }

}
