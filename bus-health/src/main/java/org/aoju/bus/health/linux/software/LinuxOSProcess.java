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

import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.RegEx;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.toolkit.StringKit;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.Executor;
import org.aoju.bus.health.Memoize;
import org.aoju.bus.health.builtin.software.AbstractOSProcess;
import org.aoju.bus.health.builtin.software.OSThread;
import org.aoju.bus.health.linux.ProcPath;
import org.aoju.bus.health.linux.drivers.ProcessStat;
import org.aoju.bus.health.linux.drivers.UserGroup;
import org.aoju.bus.health.linux.hardware.LinuxGlobalMemory;
import org.aoju.bus.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * OSProcess implemenation
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@ThreadSafe
public class LinuxOSProcess extends AbstractOSProcess {

    // Get a list of orders to pass to ParseUtil
    private static final int[] PROC_PID_STAT_ORDERS = new int[ProcPidStat.values().length];

    static {
        for (ProcPidStat stat : ProcPidStat.values()) {
            // The PROC_PID_STAT enum indices are 1-indexed.
            // Subtract one to get a zero-based index
            PROC_PID_STAT_ORDERS[stat.ordinal()] = stat.getOrder() - 1;
        }
    }

    private Supplier<String> commandLine = Memoize.memoize(this::queryCommandLine);
    private Supplier<List<String>> arguments = Memoize.memoize(this::queryArguments);
    private Supplier<Map<String, String>> environmentVariables = Memoize.memoize(this::queryEnvironmentVariables);
    private String name;
    private String path = "";
    private Supplier<Integer> bitness = Memoize.memoize(this::queryBitness);
    private String user;
    private String userID;
    private String group;
    private String groupID;
    private State state = State.INVALID;
    private int parentProcessID;
    private int threadCount;
    private int priority;
    private long virtualSize;
    private long residentSetSize;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;
    private long minorFaults;
    private long majorFaults;
    private long contextSwitches;

    public LinuxOSProcess(int pid) {
        super(pid);
        updateAttributes();
    }

    /**
     * If some details couldn't be read from ProcPath.PID_STATUS try reading it from
     * ProcPath.PID_STAT
     *
     * @param status status map to fill.
     * @param stat   string to read from.
     */
    private static void getMissingDetails(Map<String, String> status, String stat) {
        if (null == status || null == stat) {
            return;
        }

        int nameStart = stat.indexOf(Symbol.C_PARENTHESE_LEFT);
        int nameEnd = stat.indexOf(Symbol.C_PARENTHESE_RIGHT);
        if (StringKit.isBlank(status.get("Name")) && nameStart > 0 && nameStart < nameEnd) {
            // remove leading and trailing parentheses
            String statName = stat.substring(nameStart + 1, nameEnd);
            status.put("Name", statName);
        }

        // As per man, the next item after the name is the state
        if (StringKit.isBlank(status.get("State")) && nameEnd > 0 && stat.length() > nameEnd + 2) {
            String statState = String.valueOf(stat.charAt(nameEnd + 2));
            status.put("State", statState);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public String getCommandLine() {
        return commandLine.get();
    }

    private String queryCommandLine() {
        return Arrays
                .stream(Builder.getStringFromFile(String.format(ProcPath.PID_CMDLINE, getProcessID())).split("\0"))
                .collect(Collectors.joining(Symbol.SPACE));
    }

    @Override
    public List<String> getArguments() {
        return arguments.get();
    }

    private List<String> queryArguments() {
        return Collections.unmodifiableList(Builder
                .parseByteArrayToStrings(Builder.readAllBytes(String.format(ProcPath.PID_CMDLINE, getProcessID()))));
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables.get();
    }

    private Map<String, String> queryEnvironmentVariables() {
        return Collections.unmodifiableMap(Builder
                .parseByteArrayToStringMap(Builder.readAllBytes(String.format(ProcPath.PID_ENVIRON, getProcessID()))));
    }

    @Override
    public String getCurrentWorkingDirectory() {
        try {
            String cwdLink = String.format(ProcPath.PID_CWD, getProcessID());
            String cwd = new File(cwdLink).getCanonicalPath();
            if (!cwd.equals(cwdLink)) {
                return cwd;
            }
        } catch (IOException e) {
            Logger.trace("Couldn't find cwd for pid {}: {}", getProcessID(), e.getMessage());
        }
        return Normal.EMPTY;
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public String getUserID() {
        return this.userID;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public String getGroupID() {
        return this.groupID;
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    @Override
    public int getThreadCount() {
        return this.threadCount;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public long getVirtualSize() {
        return this.virtualSize;
    }

    @Override
    public long getResidentSetSize() {
        return this.residentSetSize;
    }

    @Override
    public long getKernelTime() {
        return this.kernelTime;
    }

    @Override
    public long getUserTime() {
        return this.userTime;
    }

    @Override
    public long getUpTime() {
        return this.upTime;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public long getBytesRead() {
        return this.bytesRead;
    }

    @Override
    public long getBytesWritten() {
        return this.bytesWritten;
    }

    @Override
    public long getMinorFaults() {
        return this.minorFaults;
    }

    @Override
    public long getMajorFaults() {
        return this.majorFaults;
    }

    @Override
    public long getContextSwitches() {
        return this.contextSwitches;
    }

    @Override
    public long getOpenFiles() {
        // subtract 1 from size for header
        return ProcessStat.getFileDescriptorFiles(getProcessID()).length;
    }

    @Override
    public int getBitness() {
        return this.bitness.get();
    }

    private int queryBitness() {
        // get 5th byte of file for 64-bit check
        // https://en.wikipedia.org/wiki/Executable_and_Linkable_Format#File_header
        byte[] buffer = new byte[5];
        if (!path.isEmpty()) {
            try (InputStream is = new FileInputStream(path)) {
                if (is.read(buffer) == buffer.length) {
                    return buffer[4] == 1 ? Normal._32 : Normal._64;
                }
            } catch (IOException e) {
                Logger.warn("Failed to read process file: {}", path);
            }
        }
        return 0;
    }

    @Override
    public long getAffinityMask() {
        // Would prefer to use native sched_getaffinity call but variable sizing is
        // kernel-dependent and requires C macros, so we use command line instead.
        String mask = Executor.getFirstAnswer("taskset -p " + getProcessID());
        // Output:
        // pid 3283's current affinity mask: 3
        // pid 9726's current affinity mask: f
        String[] split = RegEx.SPACES.split(mask);
        try {
            return new BigInteger(split[split.length - 1], Normal._16).longValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public List<OSThread> getThreadDetails() {
        return ProcessStat.getThreadIds(getProcessID()).stream().map(id -> new LinuxOSThread(getProcessID(), id))
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateAttributes() {
        String procPidExe = String.format(ProcPath.PID_EXE, getProcessID());
        try {
            Path link = Paths.get(procPidExe);
            this.path = Files.readSymbolicLink(link).toString();
            // For some services the symbolic link process has terminated
            int index = path.indexOf(" (deleted)");
            if (index != -1) {
                path = path.substring(0, index);
            }
        } catch (InvalidPathException | IOException | UnsupportedOperationException | SecurityException e) {
            Logger.debug("Unable to open symbolic link {}", procPidExe);
        }
        // Fetch all the values here
        // check for terminated process race condition after last one.
        Map<String, String> io = Builder.getKeyValueMapFromFile(String.format(ProcPath.PID_IO, getProcessID()), Symbol.COLON);
        Map<String, String> status = Builder.getKeyValueMapFromFile(String.format(ProcPath.PID_STATUS, getProcessID()),
                Symbol.COLON);
        String stat = Builder.getStringFromFile(String.format(ProcPath.PID_STAT, getProcessID()));
        if (stat.isEmpty()) {
            this.state = State.INVALID;
            return false;
        }

        // If some details couldn't be read from ProcPath.PID_STATUS try reading it from
        // ProcPath.PID_STAT
        getMissingDetails(status, stat);

        long now = System.currentTimeMillis();

        // We can get name and status more easily from /proc/pid/status which we
        // call later, so just get the numeric bits here
        // See man proc for how to parse /proc/[pid]/stat
        long[] statArray = Builder.parseStringToLongArray(stat, PROC_PID_STAT_ORDERS,
                ProcessStat.PROC_PID_STAT_LENGTH, Symbol.C_SPACE);

        // BOOTTIME is in seconds and start time from proc/pid/stat is in jiffies.
        // Combine units to jiffies and convert to millijiffies before hz division to
        // avoid precision loss without having to cast
        this.startTime = (LinuxOperatingSystem.BOOTTIME * LinuxOperatingSystem.getHz()
                + statArray[ProcPidStat.START_TIME.ordinal()]) * 1000L / LinuxOperatingSystem.getHz();
        // BOOT_TIME could be up to 500ms off and start time up to 5ms off. A process
        // that has started within last 505ms could produce a future start time/negative
        // up time, so insert a sanity check.
        if (startTime >= now) {
            startTime = now - 1;
        }
        this.parentProcessID = (int) statArray[ProcPidStat.PPID.ordinal()];
        this.threadCount = (int) statArray[ProcPidStat.THREAD_COUNT.ordinal()];
        this.priority = (int) statArray[ProcPidStat.PRIORITY.ordinal()];
        this.virtualSize = statArray[ProcPidStat.VSZ.ordinal()];
        this.residentSetSize = statArray[ProcPidStat.RSS.ordinal()] * LinuxGlobalMemory.PAGE_SIZE;
        this.kernelTime = statArray[ProcPidStat.KERNEL_TIME.ordinal()] * 1000L / LinuxOperatingSystem.getHz();
        this.userTime = statArray[ProcPidStat.USER_TIME.ordinal()] * 1000L / LinuxOperatingSystem.getHz();
        this.minorFaults = statArray[ProcPidStat.MINOR_FAULTS.ordinal()];
        this.majorFaults = statArray[ProcPidStat.MAJOR_FAULTS.ordinal()];
        long nonVoluntaryContextSwitches = Builder.parseLongOrDefault(status.get("nonvoluntary_ctxt_switches"), 0L);
        long voluntaryContextSwitches = Builder.parseLongOrDefault(status.get("voluntary_ctxt_switches"), 0L);
        this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;

        this.upTime = now - startTime;

        // See man proc for how to parse /proc/[pid]/io
        this.bytesRead = Builder.parseLongOrDefault(io.getOrDefault("read_bytes", Normal.EMPTY), 0L);
        this.bytesWritten = Builder.parseLongOrDefault(io.getOrDefault("write_bytes", Normal.EMPTY), 0L);

        // Don't set open files or bitness or currentWorkingDirectory; fetch on demand.

        this.userID = RegEx.SPACES.split(status.getOrDefault("Uid", Normal.EMPTY))[0];
        this.user = UserGroup.getUser(userID);
        this.groupID = RegEx.SPACES.split(status.getOrDefault("Gid", Normal.EMPTY))[0];
        this.group = UserGroup.getGroupName(groupID);
        this.name = status.getOrDefault("Name", Normal.EMPTY);
        this.state = ProcessStat.getState(status.getOrDefault("State", "U").charAt(0));
        return true;
    }

    /**
     * Enum used to update attributes. The order field represents the 1-indexed
     * numeric order of the stat in /proc/pid/stat per the man file.
     */
    private enum ProcPidStat {
        // The parsing implementation in Builder requires these to be declared
        // in increasing order
        PPID(4), MINOR_FAULTS(10), MAJOR_FAULTS(12), USER_TIME(14), KERNEL_TIME(15), PRIORITY(18), THREAD_COUNT(20),
        START_TIME(22), VSZ(23), RSS(24);

        private final int order;

        ProcPidStat(int order) {
            this.order = order;
        }

        public int getOrder() {
            return this.order;
        }
    }

}
