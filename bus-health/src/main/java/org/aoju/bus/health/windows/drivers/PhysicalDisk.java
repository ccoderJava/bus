/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org OSHI and other contributors.                 *
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
package org.aoju.bus.health.windows.drivers;

import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.tuple.Pair;
import org.aoju.bus.health.windows.PerfCounterQuery;
import org.aoju.bus.health.windows.PerfCounterWildcardQuery;
import org.aoju.bus.health.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;

import java.util.List;
import java.util.Map;

/**
 * Utility to query PhysicalDisk performance counter
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@ThreadSafe
public final class PhysicalDisk {

    private static final String PHYSICAL_DISK = "PhysicalDisk";
    private static final String WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NOT_NAME_TOTAL = "Win32_PerfRawData_PerfDisk_PhysicalDisk WHERE NOT Name=\"_Total\"";

    private PhysicalDisk() {
    }

    /**
     * Returns physical disk performance counters.
     *
     * @return Performance Counters for physical disks.
     */
    public static Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> queryDiskCounters() {
        return PerfCounterWildcardQuery.queryInstancesAndValues(PhysicalDiskProperty.class, PHYSICAL_DISK,
                WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NOT_NAME_TOTAL);
    }

    /**
     * Physical Disk performance counters.
     */
    public enum PhysicalDiskProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCE),
        // Remaining elements define counters
        DISKREADSPERSEC("Disk Reads/sec"),
        DISKREADBYTESPERSEC("Disk Read Bytes/sec"),
        DISKWRITESPERSEC("Disk Writes/sec"),
        DISKWRITEBYTESPERSEC("Disk Write Bytes/sec"),
        CURRENTDISKQUEUELENGTH("Current Disk Queue Length"),
        PERCENTDISKTIME("% Disk Time");

        private final String counter;

        PhysicalDiskProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

}
