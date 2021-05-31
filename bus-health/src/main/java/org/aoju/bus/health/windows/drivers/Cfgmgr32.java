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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;
import org.aoju.bus.core.annotation.ThreadSafe;

/**
 * Utility to query WMI class {@code Win32_USBController}
 *
 * @author Kimi Liu
 * @version 6.2.3
 * @since JDK 1.8+
 */
@ThreadSafe

public interface Cfgmgr32 extends com.sun.jna.platform.win32.Cfgmgr32 {

    Cfgmgr32 INSTANCE = Native.load("cfgmgr32", Cfgmgr32.class, W32APIOptions.DEFAULT_OPTIONS);

    int CM_DRP_DEVICEDESC = 0x00000001;
    int CM_DRP_SERVICE = 0x00000005;
    int CM_DRP_CLASS = 0x00000008;
    int CM_DRP_MFG = 0x0000000C;
    int CM_DRP_FRIENDLYNAME = 0x0000000D;

    boolean CM_Get_DevNode_Registry_Property(int dnDevInst, int ulProperty, IntByReference pulRegDataType,
                                             Pointer buffer, IntByReference pulLength, int ulFlags);
}
