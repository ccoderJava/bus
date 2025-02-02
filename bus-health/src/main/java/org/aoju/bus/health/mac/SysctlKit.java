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
package org.aoju.bus.health.mac;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.LibCAPI;
import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.logger.Logger;


/**
 * 提供对Mac OS上的sysctl调用的访问
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@ThreadSafe
public final class SysctlKit {

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    private SysctlKit() {

    }

    /**
     * 执行带有int结果的sysctl调用
     *
     * @param name 系统的名称
     * @param def  默认int值
     * @return 如果调用成功，则调用的int结果;否则默认
     */
    public static int sysctl(String name, int def) {
        LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference(com.sun.jna.platform.mac.SystemB.INT_SIZE);
        Pointer p = new Memory(size.longValue());
        if (0 != SystemB.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
            Logger.warn(SYSCTL_FAIL, name, Native.getLastError());
            return def;
        }
        return p.getInt(0);
    }

    /**
     * 执行带有长结果的sysctl调用
     *
     * @param name 系统的名称
     * @param def  默认的长整型值
     * @return 如果调用成功，则调用返回的长整型结果;否则默认
     */
    public static long sysctl(String name, long def) {
        LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference(com.sun.jna.platform.mac.SystemB.UINT64_SIZE);
        Pointer p = new Memory(size.longValue());
        if (0 != SystemB.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
            Logger.error(SYSCTL_FAIL, name, Native.getLastError());
            return def;
        }
        return p.getLong(0);
    }

    /**
     * 执行带有字符串结果的sysctl调用
     *
     * @param name 系统的名称
     * @param def  默认字符串值
     * @return 如果调用成功，则调用返回的字符串结果;否则默认
     */
    public static String sysctl(String name, String def) {
        // 第一次调用空指针来获取大小值
        LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference();
        if (0 != SystemB.INSTANCE.sysctlbyname(name, null, size, null, LibCAPI.size_t.ZERO)) {
            Logger.error(SYSCTL_FAIL, name, Native.getLastError());
            return def;
        }
        // 为空终止字符串的大小添加1
        Pointer p = new Memory(size.longValue() + 1L);
        if (0 != SystemB.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
            Logger.error(SYSCTL_FAIL, name, Native.getLastError());
            return def;
        }
        return p.getString(0);
    }

    /**
     * 执行带有结构结果的sysctl调用
     *
     * @param name   系统的名称
     * @param struct 构造结果
     * @return 如果结构成功填充为真，则为假
     */
    public static boolean sysctl(String name, Structure struct) {
        if (0 != SystemB.INSTANCE.sysctlbyname(name, struct.getPointer(), new LibCAPI.size_t.ByReference(struct.size()), null,
                LibCAPI.size_t.ZERO)) {
            Logger.error(SYSCTL_FAIL, name, Native.getLastError());
            return false;
        }
        struct.read();
        return true;
    }

    /**
     * Executes a sysctl call with a Pointer result
     *
     * @param name name of the sysctl
     * @return An allocated memory buffer containing the result on success, null
     * otherwise. Its value on failure is undefined.
     */
    public static Memory sysctl(String name) {
        LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference();
        if (0 != SystemB.INSTANCE.sysctlbyname(name, null, size, null, LibCAPI.size_t.ZERO)) {
            Logger.error(SYSCTL_FAIL, name, Native.getLastError());
            return null;
        }
        Memory m = new Memory(size.longValue());
        if (0 != SystemB.INSTANCE.sysctlbyname(name, m, size, null, LibCAPI.size_t.ZERO)) {
            Logger.error(SYSCTL_FAIL, name, Native.getLastError());
            return null;
        }
        return m;
    }

}
