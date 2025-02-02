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
package org.aoju.bus.health.unix.freebsd.hardware;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI;
import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.RegEx;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.toolkit.FileKit;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.Executor;
import org.aoju.bus.health.builtin.hardware.AbstractCentralProcessor;
import org.aoju.bus.health.builtin.hardware.CentralProcessor;
import org.aoju.bus.health.unix.freebsd.BsdSysctlKit;
import org.aoju.bus.health.unix.freebsd.FreeBsdLibc;
import org.aoju.bus.health.unix.freebsd.FreeBsdLibc.CpTime;
import org.aoju.bus.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A CPU
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@ThreadSafe
final class FreeBsdCentralProcessor extends AbstractCentralProcessor {

    private static final Pattern CPUMASK = Pattern.compile(".*<cpu\\s.*mask=\"(?:0x)?(\\p{XDigit}+)\".*>.*</cpu>.*");

    private static List<CentralProcessor.LogicalProcessor> parseTopology() {
        String[] topology = BsdSysctlKit.sysctl("kern.sched.topology_spec", Normal.EMPTY).split("\\n|\\r");
        /*-
         * Sample output:

        <groups>
        <group level="1" cache-level="0">
         <cpu count="24" mask="ffffff">0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23</cpu>
         <children>
          <group level="2" cache-level="2">
           <cpu count="12" mask="fff">0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11</cpu>
           <children>
            <group level="3" cache-level="1">
             <cpu count="2" mask="3">0, 1</cpu>
             <flags><flag name="THREAD">THREAD group</flag><flag name="SMT">SMT group</flag></flags>
            </group>

        * Opens with <groups>
        * <group> level 1 identifies all the processors via bitmask, should only be one
        * <group> level 2 separates by physical package
        * <group> level 3 puts hyperthreads together: if THREAD or SMT or HTT all the CPUs are one physical
        * If there is no level 3, then all logical processors are physical
        */
        // Create lists of the group bitmasks
        long group1 = 1L;
        List<Long> group2 = new ArrayList<>();
        List<Long> group3 = new ArrayList<>();
        int groupLevel = 0;
        for (String topo : topology) {
            if (topo.contains("<group level=")) {
                groupLevel++;
            } else if (topo.contains("</group>")) {
                groupLevel--;
            } else if (topo.contains("<cpu")) {
                // Find <cpu> tag and extract bits
                Matcher m = CPUMASK.matcher(topo);
                if (m.matches()) {
                    // Regex guarantees parsing digits so we won't get a
                    // NumberFormatException
                    switch (groupLevel) {
                        case 1:
                            group1 = Long.parseLong(m.group(1), Normal._16);
                            break;
                        case 2:
                            group2.add(Long.parseLong(m.group(1), Normal._16));
                            break;
                        case 3:
                            group3.add(Long.parseLong(m.group(1), Normal._16));
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return matchBitmasks(group1, group2, group3);
    }

    private static List<CentralProcessor.LogicalProcessor> matchBitmasks(long group1, List<Long> group2, List<Long> group3) {
        List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>();
        // Lowest and Highest set bits, indexing from 0
        int lowBit = Long.numberOfTrailingZeros(group1);
        int hiBit = 63 - Long.numberOfLeadingZeros(group1);
        // Create logical processors for this core
        for (int i = lowBit; i <= hiBit; i++) {
            if ((group1 & (1L << i)) > 0) {
                int numaNode = 0;
                CentralProcessor.LogicalProcessor logProc = new CentralProcessor.LogicalProcessor(i, getMatchingBitmask(group3, i),
                        getMatchingBitmask(group2, i), numaNode);
                logProcs.add(logProc);
            }
        }
        return logProcs;
    }

    private static int getMatchingBitmask(List<Long> bitmasks, int lp) {
        for (int j = 0; j < bitmasks.size(); j++) {
            if ((bitmasks.get(j).longValue() & (1L << lp)) != 0) {
                return j;
            }
        }
        return 0;
    }

    /**
     * Fetches the ProcessorID from dmidecode (if possible with root permissions),
     * otherwise uses the values from /var/run/dmesg.boot
     *
     * @param processorID The processorID as a long
     * @return The ProcessorID string
     */
    private static String getProcessorIDfromDmiDecode(long processorID) {
        boolean procInfo = false;
        String marker = "Processor Information";
        for (String checkLine : Executor.runNative("dmidecode -t system")) {
            if (!procInfo && checkLine.contains(marker)) {
                marker = "ID:";
                procInfo = true;
            } else if (procInfo && checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }
        // If we've gotten this far, dmidecode failed. Used the passed-in values
        return String.format("%016X", processorID);
    }

    @Override
    protected CentralProcessor.ProcessorIdentifier queryProcessorId() {
        final Pattern identifierPattern = Pattern
                .compile("Origin=\"([^\"]*)\".*Id=(\\S+).*Family=(\\S+).*Model=(\\S+).*Stepping=(\\S+).*");
        final Pattern featuresPattern = Pattern.compile("Features=(\\S+)<.*");

        String cpuVendor = Normal.EMPTY;
        String cpuName = BsdSysctlKit.sysctl("hw.model", Normal.EMPTY);
        String cpuFamily = Normal.EMPTY;
        String cpuModel = Normal.EMPTY;
        String cpuStepping = Normal.EMPTY;
        String processorID;
        long cpuFreq = BsdSysctlKit.sysctl("hw.clockrate", 0L) * 1_000_000L;

        boolean cpu64bit;

        // Parsing dmesg.boot is apparently the only reliable source for processor
        // identification in FreeBSD
        long processorIdBits = 0L;
        List<String> cpuInfo = FileKit.readLines("/var/run/dmesg.boot");
        for (String line : cpuInfo) {
            line = line.trim();
            // Prefer hw.model to this one
            if (line.startsWith("CPU:") && cpuName.isEmpty()) {
                cpuName = line.replace("CPU:", Normal.EMPTY).trim();
            } else if (line.startsWith("Origin=")) {
                Matcher m = identifierPattern.matcher(line);
                if (m.matches()) {
                    cpuVendor = m.group(1);
                    processorIdBits |= Long.decode(m.group(2));
                    cpuFamily = Integer.decode(m.group(3)).toString();
                    cpuModel = Integer.decode(m.group(4)).toString();
                    cpuStepping = Integer.decode(m.group(5)).toString();
                }
            } else if (line.startsWith("Features=")) {
                Matcher m = featuresPattern.matcher(line);
                if (m.matches()) {
                    processorIdBits |= Long.decode(m.group(1)) << Normal._32;
                }
                // No further interest in this file
                break;
            }
        }
        cpu64bit = Executor.getFirstAnswer("uname -m").trim().contains("64");
        processorID = getProcessorIDfromDmiDecode(processorIdBits);

        return new CentralProcessor.ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected List<CentralProcessor.LogicalProcessor> initProcessorCounts() {
        List<CentralProcessor.LogicalProcessor> logProcs = parseTopology();
        // Force at least one processor
        if (logProcs.isEmpty()) {
            logProcs.add(new CentralProcessor.LogicalProcessor(0, 0, 0));
        }
        return logProcs;
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[CentralProcessor.TickType.values().length];
        CpTime cpTime = new CpTime();
        BsdSysctlKit.sysctl("kern.cp_time", cpTime);
        ticks[CentralProcessor.TickType.USER.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_USER];
        ticks[CentralProcessor.TickType.NICE.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_NICE];
        ticks[CentralProcessor.TickType.SYSTEM.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_SYS];
        ticks[CentralProcessor.TickType.IRQ.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_INTR];
        ticks[CentralProcessor.TickType.IDLE.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_IDLE];
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        long[] freq = new long[1];
        freq[0] = BsdSysctlKit.sysctl("dev.cpu.0.freq", -1L);
        if (freq[0] > 0) {
            // If success, value is in MHz
            freq[0] *= 1_000_000L;
        } else {
            freq[0] = BsdSysctlKit.sysctl("machdep.tsc_freq", -1L);
        }
        return freq;
    }

    @Override
    public long queryMaxFreq() {
        long max = -1L;
        String freqLevels = BsdSysctlKit.sysctl("dev.cpu.0.freq_levels", Normal.EMPTY);
        // MHz/Watts pairs like: 2501/32000 2187/27125 2000/24000
        for (String s : RegEx.SPACES.split(freqLevels)) {
            long freq = Builder.parseLongOrDefault(s.split(Symbol.SLASH)[0], -1L);
            if (max < freq) {
                max = freq;
            }
        }
        if (max > 0) {
            // If success, value is in MHz
            max *= 1_000_000;
        } else {
            max = BsdSysctlKit.sysctl("machdep.tsc_freq", -1L);
        }
        return max;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = FreeBsdLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
                average[i] = -1d;
            }
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][CentralProcessor.TickType.values().length];

        // Allocate memory for array of CPTime
        long size = new CpTime().size();
        long arraySize = size * getLogicalProcessorCount();
        Pointer p = new Memory(arraySize);
        String name = "kern.cp_times";
        // Fetch
        if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, new LibCAPI.size_t.ByReference(new LibCAPI.size_t(arraySize)), null,
                LibCAPI.size_t.ZERO)) {
            Logger.error("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
            return ticks;
        }
        // p now points to the data; need to copy each element
        for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
            ticks[cpu][CentralProcessor.TickType.USER.getIndex()] = p
                    .getLong(size * cpu + FreeBsdLibc.CP_USER * FreeBsdLibc.UINT64_SIZE); // lgtm
            ticks[cpu][CentralProcessor.TickType.NICE.getIndex()] = p
                    .getLong(size * cpu + FreeBsdLibc.CP_NICE * FreeBsdLibc.UINT64_SIZE); // lgtm
            ticks[cpu][CentralProcessor.TickType.SYSTEM.getIndex()] = p
                    .getLong(size * cpu + FreeBsdLibc.CP_SYS * FreeBsdLibc.UINT64_SIZE); // lgtm
            ticks[cpu][CentralProcessor.TickType.IRQ.getIndex()] = p.getLong(size * cpu + FreeBsdLibc.CP_INTR * FreeBsdLibc.UINT64_SIZE); // lgtm
            ticks[cpu][CentralProcessor.TickType.IDLE.getIndex()] = p
                    .getLong(size * cpu + FreeBsdLibc.CP_IDLE * FreeBsdLibc.UINT64_SIZE); // lgtm
        }
        return ticks;
    }

    @Override
    public long queryContextSwitches() {
        String name = "vm.stats.sys.v_swtch";
        LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference(new LibCAPI.size_t(FreeBsdLibc.INT_SIZE));
        Pointer p = new Memory(size.longValue());
        if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
            return 0L;
        }
        return Builder.unsignedIntToLong(p.getInt(0));
    }

    @Override
    public long queryInterrupts() {
        String name = "vm.stats.sys.v_intr";
        LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference(new LibCAPI.size_t(FreeBsdLibc.INT_SIZE));
        Pointer p = new Memory(size.longValue());
        if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
            return 0L;
        }
        return Builder.unsignedIntToLong(p.getInt(0));
    }

}
