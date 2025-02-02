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
import org.aoju.bus.core.lang.RegEx;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.toolkit.FileKit;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.Config;
import org.aoju.bus.health.Executor;
import org.aoju.bus.health.builtin.hardware.AbstractCentralProcessor;
import org.aoju.bus.health.builtin.hardware.CentralProcessor;
import org.aoju.bus.health.linux.LinuxLibc;
import org.aoju.bus.health.linux.ProcPath;
import org.aoju.bus.health.linux.drivers.CpuStat;
import org.aoju.bus.health.linux.drivers.Lshw;
import org.aoju.bus.health.linux.software.LinuxOperatingSystem;

import java.io.File;
import java.util.*;
import java.util.stream.LongStream;

/**
 * A CPU as defined in Linux /proc.
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@ThreadSafe
final class LinuxCentralProcessor extends AbstractCentralProcessor {

    // See https://www.kernel.org/doc/Documentation/cpu-freq/user-guide.txt
    private static final String CPUFREQ_PATH = "health.cpu.freq.path";

    private static Map<Integer, Integer> mapNumaNodes() {
        Map<Integer, Integer> numaNodeMap = new HashMap<>();
        // Get numa node info from lscpu
        List<String> lscpu = Executor.runNative("lscpu -p=cpu,node");
        // Format:
        // # comment lines starting with #
        // # then comma-delimited cpu,node
        // 0,0
        // 1,0
        for (String line : lscpu) {
            if (line.startsWith(Symbol.SHAPE)) {
                continue;
            }
            String[] split = line.split(Symbol.COMMA);
            if (split.length == 2) {
                numaNodeMap.put(Builder.parseIntOrDefault(split[0], 0), Builder.parseIntOrDefault(split[1], 0));
            }
        }
        return numaNodeMap;
    }

    /**
     * Fetches the ProcessorID from dmidecode (if possible with root permissions),
     * the cpuid command (if installed) or by encoding the stepping, model, family,
     * and feature flags.
     *
     * @param vendor   The vendor
     * @param stepping The stepping
     * @param model    The model
     * @param family   The family
     * @param flags    The flags
     * @return The Processor ID string
     */
    private static String getProcessorID(String vendor, String stepping, String model, String family, String[] flags) {
        boolean procInfo = false;
        String marker = "Processor Information";
        for (String checkLine : Executor.runNative("dmidecode -t 4")) {
            if (!procInfo && checkLine.contains(marker)) {
                marker = "ID:";
                procInfo = true;
            } else if (procInfo && checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }
        // If we've gotten this far, dmidecode failed. Try cpuid.
        marker = "eax=";
        for (String checkLine : Executor.runNative("cpuid -1r")) {
            if (checkLine.contains(marker) && checkLine.trim().startsWith("0x00000001")) {
                String eax = Normal.EMPTY;
                String edx = Normal.EMPTY;
                for (String register : RegEx.SPACES.split(checkLine)) {
                    if (register.startsWith("eax=")) {
                        eax = Builder.removeMatchingString(register, "eax=0x");
                    } else if (register.startsWith("edx=")) {
                        edx = Builder.removeMatchingString(register, "edx=0x");
                    }
                }
                return edx + eax;
            }
        }
        // If we've gotten this far, dmidecode failed. Encode arguments
        if (vendor.startsWith("0x")) {
            return createMIDR(vendor, stepping, model, family) + "00000000";
        }
        return createProcessorID(stepping, model, family, flags);
    }

    /**
     * Creates the MIDR, the ARM equivalent of CPUID ProcessorID
     *
     * @param vendor   the CPU implementer
     * @param stepping the "rnpn" variant and revision
     * @param model    the partnum
     * @param family   the architecture
     * @return A 32-bit hex string for the MIDR
     */
    private static String createMIDR(String vendor, String stepping, String model, String family) {
        int midrBytes = 0;
        // Build 32-bit MIDR
        if (stepping.startsWith("r") && stepping.contains("p")) {
            String[] rev = stepping.substring(1).split("p");
            // 3:0 – Revision: last n in rnpn
            midrBytes |= Builder.parseLastInt(rev[1], 0);
            // 23:20 - Variant: first n in rnpn
            midrBytes |= Builder.parseLastInt(rev[0], 0) << 20;
        }
        // 15:4 - PartNum = model
        midrBytes |= Builder.parseLastInt(model, 0) << 4;
        // 19:16 - Architecture = family
        midrBytes |= Builder.parseLastInt(family, 0) << Normal._16;
        // 31:24 - Implementer = vendor
        midrBytes |= Builder.parseLastInt(vendor, 0) << 24;

        return String.format("%08X", midrBytes);
    }

    @Override
    protected CentralProcessor.ProcessorIdentifier queryProcessorId() {
        String cpuVendor = Normal.EMPTY;
        String cpuName = Normal.EMPTY;
        String cpuFamily = Normal.EMPTY;
        String cpuModel = Normal.EMPTY;
        String cpuStepping = Normal.EMPTY;
        long cpuFreq = 0L;
        String processorID;
        boolean cpu64bit = false;

        StringBuilder armStepping = new StringBuilder(); // For ARM equivalent
        String[] flags = new String[0];
        List<String> cpuInfo = FileKit.readLines(ProcPath.CPUINFO);
        for (String line : cpuInfo) {
            String[] splitLine = RegEx.SPACES_COLON_SPACE.split(line);
            if (splitLine.length < 2) {
                // special case
                if (line.startsWith("CPU architecture: ")) {
                    cpuFamily = line.replace("CPU architecture: ", Normal.EMPTY).trim();
                }
                continue;
            }
            switch (splitLine[0]) {
                case "vendor_id":
                case "CPU implementer":
                    cpuVendor = splitLine[1];
                    break;
                case "model name":
                    cpuName = splitLine[1];
                    break;
                case "flags":
                    flags = splitLine[1].toLowerCase().split(Symbol.SPACE);
                    for (String flag : flags) {
                        if ("lm".equals(flag)) {
                            cpu64bit = true;
                            break;
                        }
                    }
                    break;
                case "stepping":
                    cpuStepping = splitLine[1];
                    break;
                case "CPU variant":
                    if (!armStepping.toString().startsWith("r")) {
                        armStepping.insert(0, "r" + splitLine[1]);
                    }
                    break;
                case "CPU revision":
                    if (!armStepping.toString().contains("p")) {
                        armStepping.append('p').append(splitLine[1]);
                    }
                    break;
                case "model":
                case "CPU part":
                    cpuModel = splitLine[1];
                    break;
                case "cpu family":
                    cpuFamily = splitLine[1];
                    break;
                case "cpu MHz":
                    cpuFreq = Builder.parseHertz(splitLine[1]);
                    break;
                default:
                    // Do nothing
            }
        }
        if (cpuName.contains("Hz")) {
            // if Name contains CPU vendor frequency, ignore cpuinfo and use it
            cpuFreq = -1L;
        } else {
            // Try lshw and use it in preference to cpuinfo
            long cpuCapacity = Lshw.queryCpuCapacity();
            if (cpuCapacity > cpuFreq) {
                cpuFreq = cpuCapacity;
            }
        }
        if (cpuStepping.isEmpty()) {
            cpuStepping = armStepping.toString();
        }
        processorID = getProcessorID(cpuVendor, cpuStepping, cpuModel, cpuFamily, flags);
        if (cpuVendor.startsWith("0x")) {
            List<String> lscpu = Executor.runNative("lscpu");
            for (String line : lscpu) {
                if (line.startsWith("Architecture:")) {
                    cpuVendor = line.replace("Architecture:", Normal.EMPTY).trim();
                }
            }
        }
        return new CentralProcessor.ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected List<CentralProcessor.LogicalProcessor> initProcessorCounts() {
        Map<Integer, Integer> numaNodeMap = mapNumaNodes();
        List<String> procCpu = FileKit.readLines(ProcPath.CPUINFO);
        List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>();
        int currentProcessor = 0;
        int currentCore = 0;
        int currentPackage = 0;
        boolean first = true;
        for (String cpu : procCpu) {
            // Count logical processors
            if (cpu.startsWith("processor")) {
                if (!first) {
                    logProcs.add(new CentralProcessor.LogicalProcessor(currentProcessor, currentCore, currentPackage,
                            numaNodeMap.getOrDefault(currentProcessor, 0)));
                } else {
                    first = false;
                }
                currentProcessor = Builder.parseLastInt(cpu, 0);
            } else if (cpu.startsWith("core id") || cpu.startsWith("cpu number")) {
                // Count unique combinations of core id and physical id.
                currentCore = Builder.parseLastInt(cpu, 0);
            } else if (cpu.startsWith("physical id")) {
                currentPackage = Builder.parseLastInt(cpu, 0);
            }
        }
        logProcs.add(new CentralProcessor.LogicalProcessor(currentProcessor, currentCore, currentPackage,
                numaNodeMap.getOrDefault(currentProcessor, 0)));

        return logProcs;
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        // convert the Linux Jiffies to Milliseconds.
        long[] ticks = CpuStat.getSystemCpuLoadTicks();
        // In rare cases, /proc/stat reading fails. If so, try again.
        if (LongStream.of(ticks).sum() == 0) {
            ticks = CpuStat.getSystemCpuLoadTicks();
        }
        long hz = LinuxOperatingSystem.getHz();
        for (int i = 0; i < ticks.length; i++) {
            ticks[i] = ticks[i] * 1000L / hz;
        }
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        String cpuFreqPath = Config.get(CPUFREQ_PATH, Normal.EMPTY);
        long[] freqs = new long[getLogicalProcessorCount()];
        // Attempt to fill array from cpu-freq source
        long max = 0L;
        for (int i = 0; i < freqs.length; i++) {
            freqs[i] = Builder.getLongFromFile(cpuFreqPath + "/cpu" + i + "/cpufreq/scaling_cur_freq");
            if (freqs[i] == 0) {
                freqs[i] = Builder.getLongFromFile(cpuFreqPath + "/cpu" + i + "/cpufreq/cpuinfo_cur_freq");
            }
            if (max < freqs[i]) {
                max = freqs[i];
            }
        }
        if (max > 0L) {
            // If successful, array is filled with values in KHz.
            for (int i = 0; i < freqs.length; i++) {
                freqs[i] *= 1000L;
            }
            return freqs;
        }
        // If unsuccessful, try from /proc/cpuinfo
        Arrays.fill(freqs, -1);
        List<String> cpuInfo = FileKit.readLines(ProcPath.CPUINFO);
        int proc = 0;
        for (String s : cpuInfo) {
            if (s.toLowerCase().contains("cpu mhz")) {
                freqs[proc] = Math.round(Builder.parseLastDouble(s, 0d) * 1_000_000d);
                if (++proc >= freqs.length) {
                    break;
                }
            }
        }
        return freqs;
    }

    @Override
    public long queryMaxFreq() {
        String cpuFreqPath = Config.get(CPUFREQ_PATH, Normal.EMPTY);
        long max = Arrays.stream(this.getCurrentFreq()).max().orElse(-1L);
        // Max of current freq, if populated, is in units of Hz, convert to kHz
        if (max > 0) {
            max /= 1000L;
        }
        // Iterating CPUs only gets the existing policy, so we need to iterate the
        // policy directories to find the system-wide policy max
        File cpufreqdir = new File(cpuFreqPath + "/cpufreq");
        File[] policies = cpufreqdir.listFiles();
        if (null != policies) {
            for (int i = 0; i < policies.length; i++) {
                File f = policies[i];
                if (f.getName().startsWith("policy")) {
                    long freq = Builder.getLongFromFile(cpuFreqPath + "/cpufreq/" + f.getName() + "/scaling_max_freq");
                    if (freq == 0) {
                        freq = Builder.getLongFromFile(cpuFreqPath + "/cpufreq/" + f.getName() + "/cpuinfo_max_freq");
                    }
                    if (max < freq) {
                        max = freq;
                    }
                }
            }
        }
        if (max > 0L) {
            // If successful, value is in KHz.
            max *= 1000L;
            // Cpufreq result assumes intel pstates and is unreliable for AMD processors.
            // Check lshw as a backup
            long lshwMax = Lshw.queryCpuCapacity();
            return lshwMax > max ? lshwMax : max;
        }
        return -1L;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = LinuxLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
                average[i] = -1d;
            }
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = CpuStat.getProcessorCpuLoadTicks(getLogicalProcessorCount());
        // In rare cases, /proc/stat reading fails. If so, try again.
        // In theory we should check all of them, but on failure we can expect all 0's
        // so we only need to check for processor 0
        if (LongStream.of(ticks[0]).sum() == 0) {
            ticks = CpuStat.getProcessorCpuLoadTicks(getLogicalProcessorCount());
        }
        // convert the Linux Jiffies to Milliseconds.
        long hz = LinuxOperatingSystem.getHz();
        for (int i = 0; i < ticks.length; i++) {
            for (int j = 0; j < ticks[i].length; j++) {
                ticks[i][j] = ticks[i][j] * 1000L / hz;
            }
        }
        return ticks;
    }

    @Override
    public long queryContextSwitches() {
        return CpuStat.getContextSwitches();
    }

    @Override
    public long queryInterrupts() {
        return CpuStat.getInterrupts();
    }

}
