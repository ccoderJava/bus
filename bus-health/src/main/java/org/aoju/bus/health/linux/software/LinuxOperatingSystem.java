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
package org.aoju.bus.health.linux.software;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.LibC;
import com.sun.jna.platform.linux.LibC.Sysinfo;
import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.RegEx;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.lang.tuple.Pair;
import org.aoju.bus.core.lang.tuple.Triple;
import org.aoju.bus.core.toolkit.FileKit;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.Executor;
import org.aoju.bus.health.builtin.software.*;
import org.aoju.bus.health.builtin.software.OSProcess.State;
import org.aoju.bus.health.linux.LinuxLibc;
import org.aoju.bus.health.linux.ProcPath;
import org.aoju.bus.health.linux.drivers.CpuStat;
import org.aoju.bus.health.linux.drivers.ProcessStat;
import org.aoju.bus.health.linux.drivers.UpTime;
import org.aoju.bus.health.linux.drivers.Who;
import org.aoju.bus.logger.Logger;

import java.io.File;
import java.util.*;

/**
 * Linux is a family of open source Unix-like operating systems based on the
 * Linux kernel, an operating system kernel first released on September 17,
 * 1991, by Linus Torvalds. Linux is typically packaged in a Linux distribution.
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@ThreadSafe
public class LinuxOperatingSystem extends AbstractOperatingSystem {

    // Package private for access from LinuxOSProcess
    static final long BOOTTIME;
    private static final String OS_RELEASE_LOG = "os-release: {}";
    private static final String LSB_RELEASE_A_LOG = "lsb_release -a: {}";
    private static final String LSB_RELEASE_LOG = "lsb-release: {}";
    private static final String RELEASE_DELIM = " release ";
    private static final String DOUBLE_QUOTES = "(?:^\")|(?:\"$)";
    /**
     * Jiffies per second, used for process time counters.
     */
    private static final long USER_HZ = Builder.parseLongOrDefault(Executor.getFirstAnswer("getconf CLK_TCK"),
            100L);
    // PPID is 4th numeric value in proc pid stat; subtract 1 for 0-index
    private static final int[] PPID_INDEX = {3};

    static {
        long tempBT = CpuStat.getBootTime();
        // If above fails, current time minus uptime.
        if (tempBT == 0) {
            tempBT = System.currentTimeMillis() / 1000L - (long) UpTime.getSystemUptimeSeconds();
        }
        BOOTTIME = tempBT;
    }

    /**
     * <p>
     * Constructor for LinuxOperatingSystem.
     * </p>
     */
    public LinuxOperatingSystem() {
        super.getVersionInfo();
    }

    private static int getParentPidFromProcFile(int pid) {
        String stat = Builder.getStringFromFile(String.format("/proc/%d/stat", pid));
        // A race condition may leave us with an empty string
        if (stat.isEmpty()) {
            return 0;
        }
        // Grab PPID
        long[] statArray = Builder.parseStringToLongArray(stat, PPID_INDEX, ProcessStat.PROC_PID_STAT_LENGTH, Symbol.C_SPACE);
        return (int) statArray[0];
    }

    /**
     * Looks for a collection of possible distrib-release filenames
     *
     * @return The first valid matching filename
     */
    protected static String getReleaseFilename() {
        // Look for any /etc/*-release, *-version, and variants
        File etc = new File("/etc");
        // Find any *_input files in that path
        File[] matchingFiles = etc.listFiles(//
                f -> (f.getName().endsWith("-release") || //
                        f.getName().endsWith("-version") || //
                        f.getName().endsWith("_release") || //
                        f.getName().endsWith("_version")) //
                        && !(f.getName().endsWith("os-release") || //
                        f.getName().endsWith("lsb-release") || //
                        f.getName().endsWith("system-release")));
        if (null != matchingFiles && matchingFiles.length > 0) {
            return matchingFiles[0].getPath();
        }
        if (new File("/etc/release").exists()) {
            return "/etc/release";
        }
        // If all else fails, try this
        return "/etc/issue";
    }


    /**
     * Gets Jiffies per second, useful for converting ticks to milliseconds and vice
     * versa.
     *
     * @return Jiffies per second.
     */
    public static long getHz() {
        return USER_HZ;
    }

    /**
     * Converts a portion of a filename (e.g. the 'redhat' in /etc/redhat-release)
     * to a mixed case string representing the family (e.g., Red Hat)
     *
     * @param name Stripped version of filename after removing /etc and -release
     * @return Mixed case family
     */
    private static String filenameToFamily(String name) {
        switch (name.toLowerCase()) {
            // Handle known special cases
            case Normal.EMPTY:
                return "Solaris";
            case "blackcat":
                return "Black Cat";
            case "bluewhite64":
                return "BlueWhite64";
            case "e-smith":
                return "SME Server";
            case "eos":
                return "FreeEOS";
            case "hlfs":
                return "HLFS";
            case "lfs":
                return "Linux-From-Scratch";
            case "linuxppc":
                return "Linux-PPC";
            case "meego":
                return "MeeGo";
            case "mandakelinux":
                return "Mandrake";
            case "mklinux":
                return "MkLinux";
            case "nld":
                return "Novell Linux Desktop";
            case "novell":
            case "SuSE":
                return "SUSE Linux";
            case "pld":
                return "PLD";
            case "redhat":
                return "Red Hat Linux";
            case "sles":
                return "SUSE Linux ES9";
            case "sun":
                return "Sun JDS";
            case "synoinfo":
                return "Synology";
            case "tinysofa":
                return "Tiny Sofa";
            case "turbolinux":
                return "TurboLinux";
            case "ultrapenguin":
                return "UltraPenguin";
            case "va":
                return "VA-Linux";
            case "vmware":
                return "VMWareESX";
            case "yellowdog":
                return "Yellow Dog";

            // /etc/issue will end up here:
            case "issue":
                return "Unknown";
            // If not a special case just capitalize first letter
            default:
                return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }

    private static List<OSProcess> queryProcessList(Set<Integer> descendantPids) {
        List<OSProcess> procs = new ArrayList<>();
        for (int pid : descendantPids) {
            OSProcess proc = new LinuxOSProcess(pid);
            if (!proc.getState().equals(State.INVALID)) {
                procs.add(proc);
            }
        }
        return procs;
    }

    private static Map<Integer, Integer> getParentPidsFromProcFiles(File[] pidFiles) {
        Map<Integer, Integer> parentPidMap = new HashMap<>();
        for (File procFile : pidFiles) {
            int pid = Builder.parseIntOrDefault(procFile.getName(), 0);
            parentPidMap.put(pid, getParentPidFromProcFile(pid));
        }
        return parentPidMap;
    }

    private static Triple<String, String, String> queryFamilyVersionCodenameFromReleaseFiles() {
        Triple<String, String, String> familyVersionCodename;
        // There are two competing options for family/version information.
        // Newer systems are adopting a standard /etc/os-release file:
        // https://www.freedesktop.org/software/systemd/man/os-release.html
        //
        // Some systems are still using the lsb standard which parses a
        // variety of /etc/*-release files and is most easily accessed via
        // the commandline lsb_release -a, see here:
        // https://linux.die.net/man/1/lsb_release
        // In this case, the /etc/lsb-release file (if it exists) has
        // optional overrides to the information in the /etc/distrib-release
        // files, which show: "Distributor release x.x (Codename)"

        // Attempt to read /etc/system-release which has more details than
        // os-release on (CentOS and Fedora)
        if (null != (familyVersionCodename = readDistribRelease("/etc/system-release"))) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        // Attempt to read /etc/os-release file.
        if (null != (familyVersionCodename = readOsRelease())) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        // Attempt to execute the `lsb_release` command
        if (null != (familyVersionCodename = execLsbRelease())) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        // The above two options should hopefully work on most
        // distributions. If not, we keep having fun.
        // Attempt to read /etc/lsb-release file
        if (null != (familyVersionCodename = readLsbRelease())) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        // If we're still looking, we search for any /etc/*-release (or
        // similar) filename, for which the first line should be of the
        // "Distributor release x.x (Codename)" format or possibly a
        // "Distributor VERSION x.x (Codename)" format
        String etcDistribRelease = getReleaseFilename();
        if (null != (familyVersionCodename = readDistribRelease(etcDistribRelease))) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }
        // If we've gotten this far with no match, use the distrib-release
        // filename (defaults will eventually give "Unknown")
        String family = filenameToFamily(etcDistribRelease.replace("/etc/", Normal.EMPTY).replace("release", Normal.EMPTY)
                .replace("version", Normal.EMPTY).replace(Symbol.MINUS, Normal.EMPTY).replace(Symbol.UNDERLINE, Normal.EMPTY));
        return Triple.of(family, Normal.UNKNOWN, Normal.UNKNOWN);
    }

    /**
     * Attempts to read /etc/os-release
     *
     * @return a triplet with the parsed family, versionID and codeName if file
     * successfully read and NAME= found, null otherwise
     */
    private static Triple<String, String, String> readOsRelease() {
        String family = null;
        String versionId = Normal.UNKNOWN;
        String codeName = Normal.UNKNOWN;
        List<String> osRelease = FileKit.readLines("/etc/os-release");
        // Search for NAME=
        for (String line : osRelease) {
            if (line.startsWith("VERSION=")) {
                Logger.debug(OS_RELEASE_LOG, line);
                // remove beginning and ending '"' characters, etc from
                // VERSION="14.04.4 LTS, Trusty Tahr" (Ubuntu style)
                // or VERSION="17 (Beefy Miracle)" (os-release doc style)
                line = line.replace("VERSION=", Normal.EMPTY).replaceAll(DOUBLE_QUOTES, Normal.EMPTY).trim();
                String[] split = line.split("[()]");
                if (split.length <= 1) {
                    // If no parentheses, check for Ubuntu's comma format
                    split = line.split(", ");
                }
                if (split.length > 0) {
                    versionId = split[0].trim();
                }
                if (split.length > 1) {
                    codeName = split[1].trim();
                }
            } else if (line.startsWith("NAME=") && null == family) {
                Logger.debug(OS_RELEASE_LOG, line);
                // remove beginning and ending '"' characters, etc from
                // NAME="Ubuntu"
                family = line.replace("NAME=", Normal.EMPTY).replaceAll(DOUBLE_QUOTES, Normal.EMPTY).trim();
            } else if (line.startsWith("VERSION_ID=") && versionId.equals(Normal.UNKNOWN)) {
                Logger.debug(OS_RELEASE_LOG, line);
                // remove beginning and ending '"' characters, etc from
                // VERSION_ID="14.04"
                versionId = line.replace("VERSION_ID=", Normal.EMPTY).replaceAll(DOUBLE_QUOTES, Normal.EMPTY).trim();
            }
        }
        return null == family ? null : Triple.of(family, versionId, codeName);
    }

    /**
     * Attempts to execute `lsb_release -a`
     *
     * @return a Triple with the parsed family, versionID and codeName if the
     * command successfully executed and Distributor ID: or Description:
     * found, null otherwise
     */
    private static Triple<String, String, String> execLsbRelease() {
        String family = null;
        String versionId = Normal.UNKNOWN;
        String codeName = Normal.UNKNOWN;
        // If description is of the format Distrib release x.x (Codename)
        // that is primary, otherwise use Distributor ID: which returns the
        // distribution concatenated, e.g., RedHat instead of Red Hat
        for (String line : Executor.runNative("lsb_release -a")) {
            if (line.startsWith("Description:")) {
                Logger.debug(LSB_RELEASE_A_LOG, line);
                line = line.replace("Description:", Normal.EMPTY).trim();
                if (line.contains(RELEASE_DELIM)) {
                    Triple<String, String, String> Triple = parseRelease(line, RELEASE_DELIM);
                    family = Triple.getLeft();
                    if (versionId.equals(Normal.UNKNOWN)) {
                        versionId = Triple.getMiddle();
                    }
                    if (codeName.equals(Normal.UNKNOWN)) {
                        codeName = Triple.getRight();
                    }
                }
            } else if (line.startsWith("Distributor ID:") && null == family) {
                Logger.debug(LSB_RELEASE_A_LOG, line);
                family = line.replace("Distributor ID:", Normal.EMPTY).trim();
            } else if (line.startsWith("Release:") && versionId.equals(Normal.UNKNOWN)) {
                Logger.debug(LSB_RELEASE_A_LOG, line);
                versionId = line.replace("Release:", Normal.EMPTY).trim();
            } else if (line.startsWith("Codename:") && codeName.equals(Normal.UNKNOWN)) {
                Logger.debug(LSB_RELEASE_A_LOG, line);
                codeName = line.replace("Codename:", Normal.EMPTY).trim();
            }
        }
        return null == family ? null : Triple.of(family, versionId, codeName);
    }

    /**
     * Attempts to read /etc/lsb-release
     *
     * @return a Triple with the parsed family, versionID and codeName if file
     * successfully read and and DISTRIB_ID or DISTRIB_DESCRIPTION, null
     * otherwise
     */
    private static Triple<String, String, String> readLsbRelease() {
        String family = null;
        String versionId = Normal.UNKNOWN;
        String codeName = Normal.UNKNOWN;
        List<String> osRelease = FileKit.readLines("/etc/lsb-release");
        // Search for NAME=
        for (String line : osRelease) {
            if (line.startsWith("DISTRIB_DESCRIPTION=")) {
                Logger.debug(LSB_RELEASE_LOG, line);
                line = line.replace("DISTRIB_DESCRIPTION=", Normal.EMPTY).replaceAll(DOUBLE_QUOTES, Normal.EMPTY).trim();
                if (line.contains(RELEASE_DELIM)) {
                    Triple<String, String, String> Triple = parseRelease(line, RELEASE_DELIM);
                    family = Triple.getLeft();
                    if (versionId.equals(Normal.UNKNOWN)) {
                        versionId = Triple.getMiddle();
                    }
                    if (codeName.equals(Normal.UNKNOWN)) {
                        codeName = Triple.getRight();
                    }
                }
            } else if (line.startsWith("DISTRIB_ID=") && null == family) {
                Logger.debug(LSB_RELEASE_LOG, line);
                family = line.replace("DISTRIB_ID=", Normal.EMPTY).replaceAll(DOUBLE_QUOTES, Normal.EMPTY).trim();
            } else if (line.startsWith("DISTRIB_RELEASE=") && versionId.equals(Normal.UNKNOWN)) {
                Logger.debug(LSB_RELEASE_LOG, line);
                versionId = line.replace("DISTRIB_RELEASE=", Normal.EMPTY).replaceAll(DOUBLE_QUOTES, Normal.EMPTY).trim();
            } else if (line.startsWith("DISTRIB_CODENAME=") && codeName.equals(Normal.UNKNOWN)) {
                Logger.debug(LSB_RELEASE_LOG, line);
                codeName = line.replace("DISTRIB_CODENAME=", Normal.EMPTY).replaceAll(DOUBLE_QUOTES, Normal.EMPTY).trim();
            }
        }
        return null == family ? null : Triple.of(family, versionId, codeName);
    }

    /**
     * Attempts to read /etc/distrib-release (for some value of distrib)
     *
     * @return a Triple with the parsed family, versionID and codeName if file
     * successfully read and " release " or " VERSION " found, null
     * otherwise
     */
    private static Triple<String, String, String> readDistribRelease(String filename) {
        if (new File(filename).exists()) {
            List<String> osRelease = FileKit.readLines(filename);
            // Search for Distrib release x.x (Codename)
            for (String line : osRelease) {
                Logger.debug("{}: {}", filename, line);
                if (line.contains(RELEASE_DELIM)) {
                    // If this parses properly we're done
                    return parseRelease(line, RELEASE_DELIM);
                } else if (line.contains(" VERSION ")) {
                    // If this parses properly we're done
                    return parseRelease(line, " VERSION ");
                }
            }
        }
        return null;
    }

    /**
     * Helper method to parse version description line style
     *
     * @param line      a String of the form "Distributor release x.x (Codename)"
     * @param splitLine A regex to split on, e.g. " release "
     * @return a Triple with the parsed family, versionID and codeName
     */
    private static Triple<String, String, String> parseRelease(String line, String splitLine) {
        String[] split = line.split(splitLine);
        String family = split[0].trim();
        String versionId = Normal.UNKNOWN;
        String codeName = Normal.UNKNOWN;
        if (split.length > 1) {
            split = split[1].split("[()]");
            if (split.length > 0) {
                versionId = split[0].trim();
            }
            if (split.length > 1) {
                codeName = split[1].trim();
            }
        }
        return Triple.of(family, versionId, codeName);
    }

    @Override
    public String queryManufacturer() {
        return "GNU/Linux";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        Triple<String, String, String> familyVersionCodename = queryFamilyVersionCodenameFromReleaseFiles();
        String buildNumber = null;
        List<String> procVersion = FileKit.readLines(ProcPath.VERSION);
        if (!procVersion.isEmpty()) {
            String[] split = RegEx.SPACES.split(procVersion.get(0));
            for (String s : split) {
                if (!"Linux".equals(s) && !"version".equals(s)) {
                    buildNumber = s;
                    break;
                }
            }
        }
        OSVersionInfo versionInfo = new OSVersionInfo(familyVersionCodename.getMiddle(), familyVersionCodename.getRight(),
                buildNumber);
        return Pair.of(familyVersionCodename.getLeft(), versionInfo);
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && !Executor.getFirstAnswer("uname -m").contains("64")) {
            return jvmBitness;
        }
        return 64;
    }

    @Override
    public FileSystem getFileSystem() {
        return new LinuxFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new LinuxInternetProtocolStats();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
    }

    @Override
    public OSProcess getProcess(int pid) {
        OSProcess proc = new LinuxOSProcess(pid);
        if (!proc.getState().equals(State.INVALID)) {
            return proc;
        }
        return null;
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return queryChildProcesses(-1);
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        File[] pidFiles = ProcessStat.getPidFiles();
        if (parentPid >= 0) {
            // Only return descendants
            return queryProcessList(getChildrenOrDescendants(getParentPidsFromProcFiles(pidFiles), parentPid, false));
        }
        Set<Integer> descendantPids = new HashSet<>();
        // Put everything in the "descendant" set
        for (File procFile : pidFiles) {
            int pid = Builder.parseIntOrDefault(procFile.getName(), -2);
            if (pid != -2) {
                descendantPids.add(pid);
            }
        }
        return queryProcessList(descendantPids);
    }

    @Override
    public List<OSProcess> queryDescendantProcesses(int parentPid) {
        File[] pidFiles = ProcessStat.getPidFiles();
        return queryProcessList(getChildrenOrDescendants(getParentPidsFromProcFiles(pidFiles), parentPid, true));
    }

    @Override
    public int getProcessId() {
        return LinuxLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        return ProcessStat.getPidFiles().length;
    }

    @Override
    public int getThreadCount() {
        try {
            Sysinfo info = new Sysinfo();
            if (0 != LibC.INSTANCE.sysinfo(info)) {
                Logger.error("Failed to get process thread count. Error code: {}", Native.getLastError());
                return 0;
            }
            return info.procs;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            Logger.error("Failed to get procs from sysinfo. {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public long getSystemUptime() {
        return (long) UpTime.getSystemUptimeSeconds();
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new LinuxNetworkParams();
    }

    @Override
    public List<OSService> getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, ProcessFiltering.ALL_PROCESSES, ProcessSorting.PID_ASC, 0)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), OSService.State.RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        boolean systemctlFound = false;
        List<String> systemctl = Executor.runNative("systemctl list-unit-files");
        for (String text : systemctl) {
            String[] split = RegEx.SPACES.split(text);
            if (split.length >= 2 && split[0].endsWith(".service") && "enabled".equals(split[1])) {
                // remove .service extension
                String name = split[0].substring(0, split[0].length() - Normal._8);
                int index = name.lastIndexOf(Symbol.C_DOT);
                String shortName = (index < 0 || index > name.length() - 2) ? name : name.substring(index + 1);
                if (!running.contains(name) && !running.contains(shortName)) {
                    OSService s = new OSService(name, 0, OSService.State.STOPPED);
                    services.add(s);
                    systemctlFound = true;
                }
            }
        }
        if (!systemctlFound) {
            // Get Directories for stopped services
            File dir = new File("/etc/init");
            if (dir.exists() && dir.isDirectory()) {
                for (File f : dir.listFiles((f, name) -> name.toLowerCase().endsWith(".conf"))) {
                    // remove .conf extension
                    String name = f.getName().substring(0, f.getName().length() - 5);
                    int index = name.lastIndexOf(Symbol.C_DOT);
                    String shortName = (index < 0 || index > name.length() - 2) ? name : name.substring(index + 1);
                    if (!running.contains(name) && !running.contains(shortName)) {
                        OSService s = new OSService(name, 0, OSService.State.STOPPED);
                        services.add(s);
                    }
                }
            } else {
                Logger.error("Directory: /etc/init does not exist");
            }
        }
        return services;
    }


}
