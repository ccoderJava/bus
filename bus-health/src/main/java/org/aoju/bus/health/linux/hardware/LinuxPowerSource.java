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
package org.aoju.bus.health.linux.hardware;

import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.toolkit.FileKit;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.builtin.hardware.AbstractPowerSource;
import org.aoju.bus.health.builtin.hardware.PowerSource;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Power Source
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@ThreadSafe
public final class LinuxPowerSource extends AbstractPowerSource {

    private static final String PS_PATH = "/sys/class/power_supply/";

    public LinuxPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
                            double psTimeRemainingEstimated, double psTimeRemainingInstant, double psPowerUsageRate, double psVoltage,
                            double psAmperage, boolean psPowerOnLine, boolean psCharging, boolean psDischarging,
                            PowerSource.CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity,
                            int psCycleCount, String psChemistry, LocalDate psManufactureDate, String psManufacturer,
                            String psSerialNumber, double psTemperature) {
        super(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated, psTimeRemainingInstant,
                psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging, psDischarging, psCapacityUnits,
                psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount, psChemistry, psManufactureDate,
                psManufacturer, psSerialNumber, psTemperature);
    }

    /**
     * Gets Battery Information
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static List<PowerSource> getPowerSources() {
        String psName;
        String psDeviceName;
        double psRemainingCapacityPercent = -1d;
        double psTimeRemainingEstimated = -1d; // -1 = unknown, -2 = unlimited
        double psTimeRemainingInstant = -1d;
        double psPowerUsageRate = 0d;
        double psVoltage = -1d;
        double psAmperage = 0d;
        boolean psPowerOnLine = false;
        boolean psCharging = false;
        boolean psDischarging = false;
        PowerSource.CapacityUnits psCapacityUnits = PowerSource.CapacityUnits.RELATIVE;
        int psCurrentCapacity = -1;
        int psMaxCapacity = -1;
        int psDesignCapacity = -1;
        int psCycleCount = -1;
        String psChemistry;
        LocalDate psManufactureDate = null;
        String psManufacturer;
        String psSerialNumber;
        double psTemperature = 0d;

        // Get list of power source names
        File f = new File(PS_PATH);
        String[] psNames = f.list();
        List<PowerSource> psList = new ArrayList<>();
        // Empty directory will give null rather than empty array, so fix
        if (null != psNames) {
            // For each power source, output various info
            for (String name : psNames) {
                // Skip if name is ADP* or AC* (AC power supply)
                if (!name.startsWith("ADP") && !name.startsWith("AC")) {
                    // Skip if can't read uevent file
                    List<String> psInfo = FileKit.readLines(PS_PATH + name + "/uevent");
                    if (psInfo.isEmpty()) {
                        continue;
                    }
                    Map<String, String> psMap = new HashMap<>();
                    for (String line : psInfo) {
                        String[] split = line.split(Symbol.EQUAL);
                        if (split.length > 1 && !split[1].isEmpty()) {
                            psMap.put(split[0], split[1]);
                        }
                    }
                    psName = psMap.getOrDefault("POWER_SUPPLY_NAME", name);
                    String status = psMap.get("POWER_SUPPLY_STATUS");
                    psCharging = "Charging".equals(status);
                    psDischarging = "Discharging".equals(status);
                    if (psMap.containsKey("POWER_SUPPLY_CAPACITY")) {
                        psRemainingCapacityPercent = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_CAPACITY"),
                                -100) / 100d;
                    }
                    if (psMap.containsKey("POWER_SUPPLY_ENERGY_NOW")) {
                        psCurrentCapacity = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_ENERGY_NOW"), -1);
                    } else if (psMap.containsKey("POWER_SUPPLY_CHARGE_NOW")) {
                        psCurrentCapacity = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_CHARGE_NOW"), -1);
                    }
                    if (psMap.containsKey("POWER_SUPPLY_ENERGY_FULL")) {
                        psCurrentCapacity = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_ENERGY_FULL"), 1);
                    } else if (psMap.containsKey("POWER_SUPPLY_CHARGE_FULL")) {
                        psCurrentCapacity = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_CHARGE_FULL"), 1);
                    }
                    if (psMap.containsKey("POWER_SUPPLY_ENERGY_FULL_DESIGN")) {
                        psMaxCapacity = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_ENERGY_FULL_DESIGN"), 1);
                    } else if (psMap.containsKey("POWER_SUPPLY_CHARGE_FULL_DESIGN")) {
                        psMaxCapacity = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_CHARGE_FULL_DESIGN"), 1);
                    }
                    if (psMap.containsKey("POWER_SUPPLY_VOLTAGE_NOW")) {
                        psVoltage = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_VOLTAGE_NOW"), -1);
                    }
                    if (psMap.containsKey("POWER_SUPPLY_POWER_NOW")) {
                        psPowerUsageRate = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_POWER_NOW"), -1);
                    }
                    if (psVoltage > 0) {
                        psAmperage = psPowerUsageRate / psVoltage;
                    }
                    if (psMap.containsKey("POWER_SUPPLY_CYCLE_COUNT")) {
                        psCycleCount = Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_CYCLE_COUNT"), -1);
                    }
                    psChemistry = psMap.getOrDefault("POWER_SUPPLY_TECHNOLOGY", Normal.UNKNOWN);
                    psDeviceName = psMap.getOrDefault("POWER_SUPPLY_MODEL_NAME", Normal.UNKNOWN);
                    psManufacturer = psMap.getOrDefault("POWER_SUPPLY_MANUFACTURER", Normal.UNKNOWN);
                    psSerialNumber = psMap.getOrDefault("POWER_SUPPLY_SERIAL_NUMBER", Normal.UNKNOWN);
                    if (Builder.parseIntOrDefault(psMap.get("POWER_SUPPLY_PRESENT"), 1) > 0) {
                        psList.add(new LinuxPowerSource(psName, psDeviceName, psRemainingCapacityPercent,
                                psTimeRemainingEstimated, psTimeRemainingInstant, psPowerUsageRate, psVoltage,
                                psAmperage, psPowerOnLine, psCharging, psDischarging, psCapacityUnits,
                                psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount, psChemistry,
                                psManufactureDate, psManufacturer, psSerialNumber, psTemperature));
                    }
                }
            }
        }
        return psList;
    }

}
