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
package org.aoju.bus.health.mac.hardware;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;
import org.aoju.bus.core.annotation.Immutable;
import org.aoju.bus.health.builtin.hardware.AbstractDisplay;
import org.aoju.bus.health.builtin.hardware.Display;
import org.aoju.bus.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A Display
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@Immutable
final class MacDisplay extends AbstractDisplay {

    /**
     * Constructor for MacDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    MacDisplay(byte[] edid) {
        super(edid);
        Logger.debug("Initialized MacDisplay");
    }

    /**
     * Gets Display Information
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();
        // Iterate IO Registry IODisplayConnect
        IOIterator serviceIterator = IOKitUtil.getMatchingServices("IODisplayConnect");
        if (null != serviceIterator) {
            CFStringRef cfEdid = CFStringRef.createCFString("IODisplayEDID");
            IORegistryEntry sdService = serviceIterator.next();
            while (null != sdService) {
                // Display properties are in a child entry
                IORegistryEntry properties = sdService.getChildEntry("IOService");
                if (null != properties) {
                    // look up the edid by key
                    CFTypeRef edidRaw = properties.createCFProperty(cfEdid);
                    if (null != edidRaw) {
                        CFDataRef edid = new CFDataRef(edidRaw.getPointer());
                        // Edid is a byte array of 128 bytes
                        int length = edid.getLength();
                        Pointer p = edid.getBytePtr();
                        displays.add(new MacDisplay(p.getByteArray(0, length)));
                        edid.release();
                    }
                    properties.release();
                }
                // iterate
                sdService.release();
                sdService = serviceIterator.next();
            }
            serviceIterator.release();
            cfEdid.release();
        }
        return displays;
    }

}
