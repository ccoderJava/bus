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
package org.aoju.bus.health.unix.solaris.software;

import com.sun.jna.platform.unix.solaris.LibKstat;
import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.RegEx;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.lang.tuple.Pair;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.Executor;
import org.aoju.bus.health.builtin.software.*;
import org.aoju.bus.health.linux.drivers.ProcessStat;
import org.aoju.bus.health.unix.solaris.KstatKit;
import org.aoju.bus.health.unix.solaris.KstatKit.KstatChain;
import org.aoju.bus.health.unix.solaris.SolarisLibc;
import org.aoju.bus.health.unix.solaris.drivers.Who;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Solaris is a non-free Unix operating system originally developed by Sun
 * Microsystems. It superseded the company's earlier SunOS in 1993. In 2010,
 * after the Sun acquisition by Oracle, it was renamed Oracle Solaris.
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@ThreadSafe
public class SolarisOperatingSystem extends AbstractOperatingSystem {

    static final String PS_COMMAND_ARGS = Arrays.stream(PsKeywords.values()).map(Enum::name).map(String::toLowerCase)
            .collect(Collectors.joining(Symbol.COMMA));
    private static final String PS_FIELDS = "s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args";
    private static final String PROCESS_LIST_FOR_PID_COMMAND = "ps -o " + PS_FIELDS + " -p ";
    private static final String PROCESS_LIST_COMMAND = "ps -eo " + PS_FIELDS;
    private static final long BOOTTIME = querySystemBootTime();

    private static List<OSProcess> queryAllProcessesFromPS() {
        return getProcessListFromPS("ps -eo " + PS_COMMAND_ARGS, -1);
    }

    private static List<OSProcess> getProcessListFromPS(String psCommand, int pid) {
        List<OSProcess> procs = new ArrayList<>();
        List<String> procList = Executor.runNative(psCommand);
        if (procList.size() > 1) {
            // Get a map by pid of prstat output
            List<String> prstatList = pid < 0 ? Executor.runNative("prstat -v 1 1")
                    : Executor.runNative("prstat -v -p " + pid + " 1 1");
            Map<String, String> prstatRowMap = new HashMap<>();
            for (String s : prstatList) {
                String row = s.trim();
                int idx = row.indexOf(Symbol.C_SPACE);
                if (idx > 0) {
                    prstatRowMap.put(row.substring(0, idx), row);
                }
            }
            // remove header row and iterate proc list
            procList.remove(0);
            for (String proc : procList) {
                Map<PsKeywords, String> psMap = Builder.stringToEnumMap(PsKeywords.class, proc.trim(), Symbol.C_SPACE);
                // Check if last (thus all) value populated
                if (psMap.containsKey(PsKeywords.ARGS)) {
                    String pidStr = psMap.get(PsKeywords.PID);
                    Map<PrstatKeywords, String> prstatMap = Builder.stringToEnumMap(PrstatKeywords.class,
                            prstatRowMap.getOrDefault(pidStr, ""), Symbol.C_SPACE);
                    procs.add(new SolarisOSProcess(pid < 0 ? Builder.parseIntOrDefault(pidStr, 0) : pid, psMap,
                            prstatMap));
                }
            }
        }
        return procs;
    }

    private static long querySystemUptime() {
        try (KstatChain kc = KstatKit.openChain()) {
            LibKstat.Kstat ksp = KstatChain.lookup("unix", 0, "system_misc");
            if (null != ksp) {
                // Snap Time is in nanoseconds; divide for seconds
                return ksp.ks_snaptime / 1_000_000_000L;
            }
        }
        return 0L;
    }

    private static long querySystemBootTime() {
        try (KstatChain kc = KstatKit.openChain()) {
            LibKstat.Kstat ksp = KstatChain.lookup("unix", 0, "system_misc");
            if (null != ksp && KstatChain.read(ksp)) {
                return KstatKit.dataLookupLong(ksp, "boot_time");
            }
        }
        return System.currentTimeMillis() / 1000L - querySystemUptime();
    }

    private static void addChildrenToDescendantSet(String parentPid, Set<String> descendantPids, boolean recurse) {
        // Get list of children
        Set<String> childPids = new HashSet<>();
        for (String s : Executor.runNative("pgrep -P " + parentPid)) {
            String pid = s.trim();
            if (!pid.equals(parentPid) && !descendantPids.contains(pid)) {
                childPids.add(pid);
            }
        }
        // Add to descendant set
        descendantPids.addAll(childPids);
        // Recurse
        if (recurse) {
            for (String pid : childPids) {
                addChildrenToDescendantSet(pid, descendantPids, true);
            }
        }
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromPS("ps -o " + PS_COMMAND_ARGS + " -p " + pid, pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    @Override
    public String queryManufacturer() {
        return "Oracle";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String[] split = RegEx.SPACES.split(Executor.getFirstAnswer("uname -rv"));
        String version = split[0];
        String buildNumber = null;
        if (split.length > 1) {
            buildNumber = split[1];
        }
        return Pair.of("SunOS", new OperatingSystem.OSVersionInfo(version, "Solaris", buildNumber));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == Normal._64) {
            return Normal._64;
        }
        return Builder.parseIntOrDefault(Executor.getFirstAnswer("isainfo -b"), Normal._32);
    }

    @Override
    public FileSystem getFileSystem() {
        return new SolarisFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new SolarisInternetProtocolStats();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
    }

    @Override
    public int getProcessId() {
        return SolarisLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        return ProcessStat.getPidFiles().length;
    }

    @Override
    public int getThreadCount() {
        List<String> threadList = Executor.runNative("ps -eLo pid");
        if (!threadList.isEmpty()) {
            // Subtract 1 for header
            return threadList.size() - 1;
        }
        return getProcessCount();
    }

    @Override
    public long getSystemUptime() {
        return querySystemUptime();
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new SolarisNetworkParams();
    }

    @Override
    public List<OSService> getServices() {
        List<OSService> services = new ArrayList<>();
        // Get legacy RC service name possibilities
        List<String> legacySvcs = new ArrayList<>();
        File dir = new File("/etc/init.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && null != (listFiles = dir.listFiles())) {
            for (File f : listFiles) {
                legacySvcs.add(f.getName());
            }
        }
        // Iterate service list
        List<String> svcs = Executor.runNative("svcs -p");
        /*-
         Output:
         STATE          STIME    FRMI
         legacy_run     23:56:49 lrc:/etc/rc2_d/S47pppd
         legacy_run     23:56:49 lrc:/etc/rc2_d/S81dodatadm_udaplt
         legacy_run     23:56:49 lrc:/etc/rc2_d/S89PRESERVE
         online         23:56:25 svc:/system/early-manifest-import:default
         online         23:56:25 svc:/system/svc/restarter:default
                        23:56:24       13 svc.startd
                        ...
         */
        for (String line : svcs) {
            if (line.startsWith("online")) {
                int delim = line.lastIndexOf(":/");
                if (delim > 0) {
                    String name = line.substring(delim + 1);
                    if (name.endsWith(":default")) {
                        name = name.substring(0, name.length() - 8);
                    }
                    services.add(new OSService(name, 0, OSService.State.STOPPED));
                }
            } else if (line.startsWith(Symbol.SPACE)) {
                String[] split = RegEx.SPACES.split(line.trim());
                if (split.length == 3) {
                    services.add(new OSService(split[2], Builder.parseIntOrDefault(split[1], 0), OSService.State.RUNNING));
                }
            } else if (line.startsWith("legacy_run")) {
                for (String svc : legacySvcs) {
                    if (line.endsWith(svc)) {
                        services.add(new OSService(svc, 0, OSService.State.STOPPED));
                        break;
                    }
                }
            }
        }
        return services;
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return queryAllProcessesFromPS();
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcessesFromPS();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, false);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    @Override
    public List<OSProcess> queryDescendantProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcessesFromPS();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, true);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    enum PsKeywords {
        S, PID, PPID, USER, UID, GROUP, GID, NLWP, PRI, VSZ, RSS, ETIME, TIME, COMM, ARGS; // ARGS must always be last
    }

    enum PrstatKeywords {
        PID, USERNAME, USR, SYS, TRP, TFL, DFL, LCK, SLP, LAT, VCX, ICX, SCL, SIG, PROCESS_NLWP; // prstat -v
    }

}
