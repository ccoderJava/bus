package org.aoju.bus.health.linux.hardware;


import com.sun.jna.platform.linux.Udev;
import org.aoju.bus.core.lang.RegEx;
import org.aoju.bus.core.toolkit.StringKit;
import org.aoju.bus.health.Executor;
import org.aoju.bus.health.builtin.hardware.AbstractLogicalVolumeGroup;
import org.aoju.bus.health.builtin.hardware.LogicalVolumeGroup;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


final class LinuxLogicalVolumeGroup extends AbstractLogicalVolumeGroup {

    private static final String BLOCK = "block";
    private static final String DM_UUID = "DM_UUID";
    private static final String DM_VG_NAME = "DM_VG_NAME";
    private static final String DM_LV_NAME = "DM_LV_NAME";
    private static final String DEV_LOCATION = "/dev/";

    LinuxLogicalVolumeGroup(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        Map<String, Map<String, Set<String>>> logicalVolumesMap = new HashMap<>();
        Map<String, Set<String>> physicalVolumesMap = new HashMap<>();

        // Populate pv map from pvs command
        // This requires elevated permissions and may fail
        for (String s : Executor.runNative("pvs -o vg_name,pv_name")) {
            String[] split = RegEx.SPACES.split(s.trim());
            if (split.length == 2 && split[1].startsWith(DEV_LOCATION)) {
                physicalVolumesMap.computeIfAbsent(split[0], k -> new HashSet<>()).add(split[1]);
            }
        }

        // Populate lv map from udev
        Udev.UdevContext udev = Udev.INSTANCE.udev_new();
        try {
            Udev.UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem(BLOCK);
                enumerate.scanDevices();
                for (Udev.UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName();
                    Udev.UdevDevice device = udev.deviceNewFromSyspath(syspath);
                    if (device != null) {
                        try {
                            String devnode = device.getDevnode();
                            if (devnode != null && devnode.startsWith("/dev/dm")) {
                                String uuid = device.getPropertyValue(DM_UUID);
                                if (uuid != null && uuid.startsWith("LVM-")) {
                                    String vgName = device.getPropertyValue(DM_VG_NAME);
                                    String lvName = device.getPropertyValue(DM_LV_NAME);
                                    if (!StringKit.isBlank(vgName) && !StringKit.isBlank(lvName)) {
                                        logicalVolumesMap.computeIfAbsent(vgName, k -> new HashMap<>());
                                        Map<String, Set<String>> lvMapForGroup = logicalVolumesMap.get(vgName);
                                        // Backup to add to pv set if pvs command failed
                                        physicalVolumesMap.computeIfAbsent(vgName, k -> new HashSet<>());
                                        Set<String> pvSetForGroup = physicalVolumesMap.get(vgName);

                                        File slavesDir = new File(syspath + "/slaves");
                                        File[] slaves = slavesDir.listFiles();
                                        if (slaves != null) {
                                            for (File f : slaves) {
                                                String pvName = f.getName();
                                                lvMapForGroup.computeIfAbsent(lvName, k -> new HashSet<>())
                                                        .add(DEV_LOCATION + pvName);
                                                // Backup to add to pv set if pvs command failed
                                                // Added /dev/ to remove duplicates like /dev/sda1 and sda1
                                                pvSetForGroup.add(DEV_LOCATION + pvName);
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            device.unref();
                        }
                    }
                }
            } finally {
                enumerate.unref();
            }
        } finally {
            udev.unref();
        }
        return logicalVolumesMap.entrySet().stream()
                .map(e -> new LinuxLogicalVolumeGroup(e.getKey(), e.getValue(), physicalVolumesMap.get(e.getKey())))
                .collect(Collectors.toList());
    }

}