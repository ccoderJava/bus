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
package org.aoju.bus.health.unix.freebsd;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.health.unix.CLibrary;

/**
 * C动态库,这个类应该被认为是非api的，因为如果/当
 * 它的代码被合并到JNA项目中时，它可能会被删除
 *
 * @author Kimi Liu
 * @version 6.3.0
 * @since JDK 1.8+
 */
public interface FreeBsdLibc extends CLibrary {

    FreeBsdLibc INSTANCE = Native.load("libc", FreeBsdLibc.class);

    int UTX_USERSIZE = Normal._32;
    int UTX_LINESIZE = Normal._16;
    int UTX_IDSIZE = 8;
    int UTX_HOSTSIZE =  Normal._128;
    /**
     * Constant <code>UINT64_SIZE=Native.getNativeSize(long.class)</code>
     */
    int UINT64_SIZE = Native.getNativeSize(long.class);

    /*
     * Data size
     */
    /**
     * Constant <code>INT_SIZE=Native.getNativeSize(int.class)</code>
     */
    int INT_SIZE = Native.getNativeSize(int.class);
    /**
     * Constant <code>CPUSTATES=5</code>
     */
    int CPUSTATES = 5;

    /*
     * CPU state indices
     */
    /**
     * Constant <code>CP_USER=0</code>
     */
    int CP_USER = 0;
    /**
     * Constant <code>CP_NICE=1</code>
     */
    int CP_NICE = 1;
    /**
     * Constant <code>CP_SYS=2</code>
     */
    int CP_SYS = 2;
    /**
     * Constant <code>CP_INTR=3</code>
     */
    int CP_INTR = 3;
    /**
     * Constant <code>CP_IDLE=4</code>
     */
    int CP_IDLE = 4;

    /**
     * Reads a line from the current file position in the utmp file. It returns a
     * pointer to a structure containing the fields of the line.
     * <p>
     * Not thread safe
     *
     * @return a {@link FreeBsdUtmpx} on success, and NULL on failure (which
     * includes the "record not found" case)
     */
    FreeBsdUtmpx getutxent();

    /**
     * Connection info
     */
    @FieldOrder({"ut_type", "ut_tv", "ut_id", "ut_pid", "ut_user", "ut_line", "ut_host", "ut_spare"})
    class FreeBsdUtmpx extends Structure {
        public short ut_type; // type of entry
        public Timeval ut_tv; // time entry was made
        public byte[] ut_id = new byte[UTX_IDSIZE]; // etc/inittab id (usually line #)
        public int ut_pid; // process id
        public byte[] ut_user = new byte[UTX_USERSIZE]; // user login name
        public byte[] ut_line = new byte[UTX_LINESIZE]; // device name
        public byte[] ut_host = new byte[UTX_HOSTSIZE]; // host name
        public byte[] ut_spare = new byte[64];
    }

    /**
     * Return type for BSD sysctl kern.boottime
     */
    @FieldOrder({"tv_sec", "tv_usec"})
    class Timeval extends Structure {
        public long tv_sec; // seconds
        public long tv_usec; // microseconds
    }

    /**
     * CPU Ticks
     */
    @FieldOrder({"cpu_ticks"})
    class CpTime extends Structure {
        public long[] cpu_ticks = new long[CPUSTATES];
    }

}
