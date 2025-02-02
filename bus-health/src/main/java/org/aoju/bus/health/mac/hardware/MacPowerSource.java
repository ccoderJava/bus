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
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.*;
import com.sun.jna.platform.mac.IOKit;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;
import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.health.builtin.hardware.AbstractPowerSource;
import org.aoju.bus.health.builtin.hardware.PowerSource;
import org.aoju.bus.health.mac.drivers.WindowInfo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A Power Source
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@ThreadSafe
public final class MacPowerSource extends AbstractPowerSource {

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;
    private static final IOKit IO = IOKit.INSTANCE;

    public MacPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
                          double psTimeRemainingEstimated, double psTimeRemainingInstant, double psPowerUsageRate, double psVoltage,
                          double psAmperage, boolean psPowerOnLine, boolean psCharging, boolean psDischarging,
                          CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity,
                          int psCycleCount, String psChemistry, LocalDate psManufactureDate, String psManufacturer,
                          String psSerialNumber, double psTemperature) {
        super(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated, psTimeRemainingInstant,
                psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging, psDischarging, psCapacityUnits,
                psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount, psChemistry, psManufactureDate,
                psManufacturer, psSerialNumber, psTemperature);
    }

    /**
     * Gets Battery Information.
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static List<PowerSource> getPowerSources() {
        String psDeviceName = Normal.UNKNOWN;
        double psTimeRemainingInstant = 0d;
        double psPowerUsageRate = 0d;
        double psVoltage = -1d;
        double psAmperage = 0d;
        boolean psPowerOnLine = false;
        boolean psCharging = false;
        boolean psDischarging = false;
        CapacityUnits psCapacityUnits = CapacityUnits.RELATIVE;
        int psCurrentCapacity = 0;
        int psMaxCapacity = 1;
        int psDesignCapacity = 1;
        int psCycleCount = -1;
        String psChemistry = Normal.UNKNOWN;
        LocalDate psManufactureDate = null;
        String psManufacturer = Normal.UNKNOWN;
        String psSerialNumber = Normal.UNKNOWN;
        double psTemperature = 0d;

        // Mac PowerSource information comes from two sources: the IOKit's IOPS
        // functions (which, in theory, return an array of objects but in most cases
        // should return one), and the IORegistry's entry for AppleSmartBattery, which
        // always returns one object.
        //
        // We start by fetching the registry information, which will be replicated
        // across all IOPS entries if there are more than one.

        IORegistryEntry smartBattery = IOKitUtil.getMatchingService("AppleSmartBattery");
        if (null != smartBattery) {
            String s = smartBattery.getStringProperty("DeviceName");
            if (null != s) {
                psDeviceName = s;
            }
            s = smartBattery.getStringProperty("Manufacturer");
            if (null != s) {
                psManufacturer = s;
            }
            s = smartBattery.getStringProperty("BatterySerialNumber");
            if (null != s) {
                psSerialNumber = s;
            }

            Integer temp = smartBattery.getIntegerProperty("ManufactureDate");
            if (null != temp) {
                // Bits 0...4 => day (value 1-31; 5 bits)
                // Bits 5...8 => month (value 1-12; 4 bits)
                // Bits 9...15 => years since 1980 (value 0-127; 7 bits)
                int day = temp & 0x1f;
                int month = (temp >> 5) & 0xf;
                int year80 = (temp >> 9) & 0x7f;
                psManufactureDate = LocalDate.of(1980 + year80, month, day);
            }

            temp = smartBattery.getIntegerProperty("DesignCapacity");
            if (null != temp) {
                psDesignCapacity = temp;
            }
            temp = smartBattery.getIntegerProperty("MaxCapacity");
            if (null != temp) {
                psMaxCapacity = temp;
            }
            temp = smartBattery.getIntegerProperty("CurrentCapacity");
            if (null != temp) {
                psCurrentCapacity = temp;
            }
            psCapacityUnits = CapacityUnits.MAH;

            temp = smartBattery.getIntegerProperty("TimeRemaining");
            if (null != temp) {
                psTimeRemainingInstant = temp * 60d;
            }
            temp = smartBattery.getIntegerProperty("CycleCount");
            if (null != temp) {
                psCycleCount = temp;
            }
            temp = smartBattery.getIntegerProperty("Temperature");
            if (null != temp) {
                psTemperature = temp / 100d;
            }
            temp = smartBattery.getIntegerProperty("Voltage");
            if (null != temp) {
                psVoltage = temp / 1000d;
            }
            temp = smartBattery.getIntegerProperty("Amperage");
            if (null != temp) {
                psAmperage = temp;
            }
            psPowerUsageRate = psVoltage * psAmperage;

            Boolean bool = smartBattery.getBooleanProperty("ExternalConnected");
            if (null != bool) {
                psPowerOnLine = bool;
            }
            bool = smartBattery.getBooleanProperty("IsCharging");
            if (null != bool) {
                psCharging = bool;
            }
            psDischarging = !psCharging;

            smartBattery.release();
        }

        // Get the blob containing current power source state
        CFTypeRef powerSourcesInfo = IO.IOPSCopyPowerSourcesInfo();
        CFArrayRef powerSourcesList = IO.IOPSCopyPowerSourcesList(powerSourcesInfo);
        int powerSourcesCount = powerSourcesList.getCount();

        // Get time remaining
        // -1 = unknown, -2 = unlimited
        double psTimeRemainingEstimated = IO.IOPSGetTimeRemainingEstimate();

        CFStringRef nameKey = CFStringRef.createCFString("Name");
        CFStringRef isPresentKey = CFStringRef.createCFString("Is Present");
        CFStringRef currentCapacityKey = CFStringRef.createCFString("Current Capacity");
        CFStringRef maxCapacityKey = CFStringRef.createCFString("Max Capacity");
        // For each power source, output various info
        List<PowerSource> psList = new ArrayList<>(powerSourcesCount);
        for (int ps = 0; ps < powerSourcesCount; ps++) {
            // Get the dictionary for that Power Source
            Pointer pwrSrcPtr = powerSourcesList.getValueAtIndex(ps);
            CFTypeRef powerSource = new CFTypeRef();
            powerSource.setPointer(pwrSrcPtr);
            CFDictionaryRef dictionary = IO.IOPSGetPowerSourceDescription(powerSourcesInfo, powerSource);

            // Get values from dictionary (See IOPSKeys.h)
            // Skip if not present
            Pointer result = dictionary.getValue(isPresentKey);
            if (null != result) {
                CFBooleanRef isPresentRef = new CFBooleanRef(result);
                if (0 != CF.CFBooleanGetValue(isPresentRef)) {
                    // Get name
                    result = dictionary.getValue(nameKey);
                    String psName = WindowInfo.cfPointerToString(result);
                    // Remaining Capacity = current / max
                    double currentCapacity = 0d;
                    if (dictionary.getValueIfPresent(currentCapacityKey, null)) {
                        result = dictionary.getValue(currentCapacityKey);
                        CFNumberRef cap = new CFNumberRef(result);
                        currentCapacity = cap.intValue();
                    }
                    double maxCapacity = 1d;
                    if (dictionary.getValueIfPresent(maxCapacityKey, null)) {
                        result = dictionary.getValue(maxCapacityKey);
                        CFNumberRef cap = new CFNumberRef(result);
                        maxCapacity = cap.intValue();
                    }
                    double psRemainingCapacityPercent = Math.min(1d, currentCapacity / maxCapacity);
                    // Add to list
                    psList.add(new MacPowerSource(psName, psDeviceName, psRemainingCapacityPercent,
                            psTimeRemainingEstimated, psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage,
                            psPowerOnLine, psCharging, psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity,
                            psDesignCapacity, psCycleCount, psChemistry, psManufactureDate, psManufacturer,
                            psSerialNumber, psTemperature));
                }
            }
        }
        isPresentKey.release();
        nameKey.release();
        currentCapacityKey.release();
        maxCapacityKey.release();
        // Release the blob
        powerSourcesList.release();
        powerSourcesInfo.release();

        return psList;
    }

}
