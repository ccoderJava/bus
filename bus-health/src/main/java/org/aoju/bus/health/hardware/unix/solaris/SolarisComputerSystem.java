/*
 * The MIT License
 *
 * Copyright (c) 2015-2020 aoju.org All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.aoju.bus.health.hardware.unix.solaris;

import org.aoju.bus.core.utils.StringUtils;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.Command;
import org.aoju.bus.health.Memoizer;
import org.aoju.bus.health.hardware.AbstractComputerSystem;
import org.aoju.bus.health.hardware.Baseboard;
import org.aoju.bus.health.hardware.Firmware;

import java.util.function.Supplier;

/**
 * Hardware data obtained from smbios.
 *
 * @author Kimi Liu
 * @version 5.5.2
 * @since JDK 1.8+
 */
final class SolarisComputerSystem extends AbstractComputerSystem {

    private final Supplier<SmbiosStrings> smbiosStrings = Memoizer.memoize(this::readSmbios);

    @Override
    public String getManufacturer() {
        return smbiosStrings.get().manufacturer;
    }

    @Override
    public String getModel() {
        return smbiosStrings.get().model;
    }

    @Override
    public String getSerialNumber() {
        return smbiosStrings.get().serialNumber;
    }

    @Override
    public Firmware createFirmware() {
        return new SolarisFirmware(smbiosStrings.get().biosVendor, smbiosStrings.get().biosVersion,
                smbiosStrings.get().biosDate);
    }

    @Override
    public Baseboard createBaseboard() {
        return new SolarisBaseboard(smbiosStrings.get().boardManufacturer, smbiosStrings.get().boardModel,
                smbiosStrings.get().boardSerialNumber, smbiosStrings.get().boardVersion);
    }

    private SmbiosStrings readSmbios() {
        String biosVendor = null;
        String biosVersion = null;
        String biosDate = null;

        String manufacturer = null;
        String model = null;
        String serialNumber = null;

        String boardManufacturer = null;
        String boardModel = null;
        String boardVersion = null;
        String boardSerialNumber = null;

        // $ smbios
        // ID SIZE TYPE
        // 0 87 SMB_TYPE_BIOS (BIOS Information)
        //
        // Vendor: Parallels Software International Inc.
        // Version String: 11.2.1 (32686)
        // Release Date: 07/15/2016
        // Address Segment: 0xf000
        // ... <snip> ...
        //
        // ID SIZE TYPE
        // 1 177 SMB_TYPE_SYSTEM (system information)
        //
        // Manufacturer: Parallels Software International Inc.
        // Product: Parallels Virtual Platforom
        // Version: None
        // Serial Number: Parallels-45 2E 7E 2D 57 5C 4B 59 B1 30 28 81 B7 81 89
        // 34
        //
        // UUID: 452e7e2d-575c04b59-b130-2881b7818934
        // Wake-up Event: 0x6 (Power Switch)
        // SKU Number: Undefined
        // Family: Parallels VM
        //
        // ID SIZE TYPE
        // 2 90 SMB_TYPE_BASEBOARD (base board)
        //
        // Manufacturer: Parallels Software International Inc.
        // Product: Parallels Virtual Platform
        // Version: None
        // Serial Number: None
        // ... <snip> ...
        //
        // ID SIZE TYPE
        // 3 .... <snip> ...

        final String vendorMarker = "Vendor:";
        final String biosDateMarker = "Release Date:";
        final String biosVersionMarker = "VersionString:";

        final String manufacturerMarker = "Manufacturer:";
        final String productMarker = "Product:";
        final String serialNumMarker = "Serial Number:";
        final String versionMarker = "Version:";

        int smbTypeId = -1;
        // Only works with root permissions but it's all we've got
        for (final String checkLine : Command.runNative("smbios")) {
            // Change the smbTypeId when hitting a new header
            if (checkLine.contains("SMB_TYPE_") && (smbTypeId = getSmbType(checkLine)) == Integer.MAX_VALUE) {
                // If we get past what we need, stop iterating
                break;
            }
            // Based on the smbTypeID we are processing for
            switch (smbTypeId) {
                case 0: // BIOS
                    if (checkLine.contains(vendorMarker)) {
                        biosVendor = checkLine.split(vendorMarker)[1].trim();
                    } else if (checkLine.contains(biosVersionMarker)) {
                        biosVersion = checkLine.split(biosVersionMarker)[1].trim();
                    } else if (checkLine.contains(biosDateMarker)) {
                        biosDate = checkLine.split(biosDateMarker)[1].trim();
                    }
                    break;
                case 1: // SYSTEM
                    if (checkLine.contains(manufacturerMarker)) {
                        manufacturer = checkLine.split(manufacturerMarker)[1].trim();
                    } else if (checkLine.contains(productMarker)) {
                        model = checkLine.split(productMarker)[1].trim();
                    } else if (checkLine.contains(serialNumMarker)) {
                        serialNumber = checkLine.split(serialNumMarker)[1].trim();
                    }
                    break;
                case 2: // BASEBOARD
                    if (checkLine.contains(manufacturerMarker)) {
                        boardManufacturer = checkLine.split(manufacturerMarker)[1].trim();
                    } else if (checkLine.contains(productMarker)) {
                        boardModel = checkLine.split(productMarker)[1].trim();
                    } else if (checkLine.contains(versionMarker)) {
                        boardVersion = checkLine.split(versionMarker)[1].trim();
                    } else if (checkLine.contains(serialNumMarker)) {
                        boardSerialNumber = checkLine.split(serialNumMarker)[1].trim();
                    }
                    break;
                default:
                    break;
            }
        }
        // If we get to end and haven't assigned, use fallback
        if (StringUtils.isBlank(serialNumber)) {
            serialNumber = readSerialNumber();
        }
        return new SmbiosStrings(biosVendor, biosVersion, biosDate, manufacturer, model, serialNumber,
                boardManufacturer, boardModel, boardVersion, boardSerialNumber);
    }

    private int getSmbType(String checkLine) {
        if (checkLine.contains("SMB_TYPE_BIOS")) {
            return 0; // BIOS
        } else if (checkLine.contains("SMB_TYPE_SYSTEM")) {
            return 1; // SYSTEM
        } else if (checkLine.contains("SMB_TYPE_BASEBOARD")) {
            return 2; // BASEBOARD
        } else {
            // First 3 SMB_TYPEs are what we need. After that no need to
            // continue processing the output
            return Integer.MAX_VALUE;
        }
    }

    private String readSerialNumber() {
        // If they've installed STB (Sun Explorer) this should work
        String serialNumber = Command.getFirstAnswer("sneep");
        // if that didn't work, try...
        if (serialNumber.isEmpty()) {
            String marker = "chassis-sn:";
            for (String checkLine : Command.runNative("prtconf -pv")) {
                if (checkLine.contains(marker)) {
                    serialNumber = Builder.getSingleQuoteStringValue(checkLine);
                    break;
                }
            }
        }
        return serialNumber;
    }

    private static final class SmbiosStrings {

        private final String biosVendor;
        private final String biosVersion;
        private final String biosDate;

        private final String manufacturer;
        private final String model;
        private final String serialNumber;

        private final String boardManufacturer;
        private final String boardModel;
        private final String boardVersion;
        private final String boardSerialNumber;

        private SmbiosStrings(String biosVendor, String biosVersion, String biosDate,
                              String manufacturer, String model, String serialNumber,
                              String boardManufacturer, String boardModel, String boardVersion, String boardSerialNumber) {
            this.biosVendor = StringUtils.isBlank(biosVendor) ? Builder.UNKNOWN : biosVendor;
            this.biosVersion = StringUtils.isBlank(biosVersion) ? Builder.UNKNOWN : biosVersion;
            this.biosDate = StringUtils.isBlank(biosDate) ? Builder.UNKNOWN : biosDate;

            this.manufacturer = StringUtils.isBlank(manufacturer) ? Builder.UNKNOWN : manufacturer;
            this.model = StringUtils.isBlank(model) ? Builder.UNKNOWN : model;
            this.serialNumber = StringUtils.isBlank(serialNumber) ? Builder.UNKNOWN : serialNumber;

            this.boardManufacturer = StringUtils.isBlank(boardManufacturer) ? Builder.UNKNOWN : boardManufacturer;
            this.boardModel = StringUtils.isBlank(boardModel) ? Builder.UNKNOWN : boardModel;
            this.boardVersion = StringUtils.isBlank(boardVersion) ? Builder.UNKNOWN : boardVersion;
            this.boardSerialNumber = StringUtils.isBlank(boardSerialNumber) ? Builder.UNKNOWN : boardSerialNumber;
        }
    }

}
