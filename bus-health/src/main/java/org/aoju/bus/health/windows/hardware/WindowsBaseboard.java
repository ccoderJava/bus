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
package org.aoju.bus.health.windows.hardware;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import org.aoju.bus.core.annotation.Immutable;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.tuple.Quartet;
import org.aoju.bus.core.toolkit.StringKit;
import org.aoju.bus.health.Memoize;
import org.aoju.bus.health.builtin.hardware.AbstractBaseboard;
import org.aoju.bus.health.windows.WmiKit;
import org.aoju.bus.health.windows.drivers.Win32BaseBoard;

import java.util.function.Supplier;

/**
 * Baseboard data obtained from WMI
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@Immutable
final class WindowsBaseboard extends AbstractBaseboard {

    private final Supplier<Quartet<String, String, String, String>> manufModelVersSerial = Memoize.memoize(
            WindowsBaseboard::queryManufModelVersSerial);

    private static Quartet<String, String, String, String> queryManufModelVersSerial() {
        String manufacturer = null;
        String model = null;
        String version = null;
        String serialNumber = null;
        WmiResult<Win32BaseBoard.BaseBoardProperty> win32BaseBoard = Win32BaseBoard.queryBaseboardInfo();
        if (win32BaseBoard.getResultCount() > 0) {
            manufacturer = WmiKit.getString(win32BaseBoard, Win32BaseBoard.BaseBoardProperty.MANUFACTURER, 0);
            model = WmiKit.getString(win32BaseBoard, Win32BaseBoard.BaseBoardProperty.MODEL, 0);
            version = WmiKit.getString(win32BaseBoard, Win32BaseBoard.BaseBoardProperty.VERSION, 0);
            serialNumber = WmiKit.getString(win32BaseBoard, Win32BaseBoard.BaseBoardProperty.SERIALNUMBER, 0);
        }
        return new Quartet<>(StringKit.isBlank(manufacturer) ? Normal.UNKNOWN : manufacturer,
                StringKit.isBlank(model) ? Normal.UNKNOWN : model, StringKit.isBlank(version) ? Normal.UNKNOWN : version,
                StringKit.isBlank(serialNumber) ? Normal.UNKNOWN : serialNumber);
    }

    @Override
    public String getManufacturer() {
        return manufModelVersSerial.get().getA();
    }

    @Override
    public String getModel() {
        return manufModelVersSerial.get().getB();
    }

    @Override
    public String getVersion() {
        return manufModelVersSerial.get().getC();
    }

    @Override
    public String getSerialNumber() {
        return manufModelVersSerial.get().getD();
    }

}
