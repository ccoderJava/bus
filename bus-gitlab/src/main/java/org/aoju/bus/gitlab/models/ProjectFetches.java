/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org Greg Messner and other contributors.         *
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
package org.aoju.bus.gitlab.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.aoju.bus.gitlab.JacksonJson;

import java.util.Date;
import java.util.List;

/**
 * @author Kimi Liu
 * @version 6.1.2
 * @since JDK 1.8+
 */
public class ProjectFetches {

    private Fetches fetches;

    public Fetches getFetches() {
        return fetches;
    }

    public void setFetches(Fetches fetches) {
        this.fetches = fetches;
    }

    @Override
    public String toString() {
        return (JacksonJson.toJsonString(this));
    }

    public static class DateCount {

        private Integer count;

        @JsonSerialize(using = JacksonJson.DateOnlySerializer.class)
        private Date date;

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    public static class Fetches {

        private Integer total;
        private List<DateCount> days;

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }

        public List<DateCount> getDays() {
            return days;
        }

        public void setDays(List<DateCount> days) {
            this.days = days;
        }
    }
}
