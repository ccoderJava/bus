/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org and other contributors.                      *
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
package org.aoju.bus.core.toolkit;

import org.aoju.bus.core.collection.EnumerationIterator;
import org.aoju.bus.core.convert.Convert;
import org.aoju.bus.core.lang.*;
import org.aoju.bus.core.lang.exception.InstrumentException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.regex.Matcher;

/**
 * 网络相关工具
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public class NetKit {

    /**
     * 默认最小端口,1024
     */
    public static final int PORT_RANGE_MIN = Normal._1024;
    /**
     * 默认最大端口,65535
     */
    public static final int PORT_RANGE_MAX = 0xFFFF;

    /**
     * 根据long值获取ip v4地址
     *
     * @param longIP IP的long表示形式
     * @return IP V4 地址
     */
    public static String longToIpv4(long longIP) {
        final StringBuilder sb = new StringBuilder();
        // 直接右移24位
        sb.append((longIP >>> 24));
        sb.append(Symbol.DOT);
        // 将高8位置0,然后右移16位
        sb.append(longIP >> Normal._16 & 0xFF);
        sb.append(Symbol.DOT);
        sb.append(longIP >> 8 & 0xFF);
        sb.append(Symbol.DOT);
        sb.append(longIP & 0xFF);
        return sb.toString();
    }

    /**
     * 根据ip地址计算出long型的数据
     *
     * @param strIP IP V4 地址
     * @return long值
     */
    public static long ipv4ToLong(String strIP) {
        final Matcher matcher = RegEx.IPV4.matcher(strIP);
        if (matcher.matches()) {
            long addr = 0;
            for (int i = 1; i <= 4; ++i) {
                addr |= Long.parseLong(matcher.group(i)) << 8 * (4 - i);
            }
            return addr;
        }
        throw new IllegalArgumentException("Invalid IPv4 address!");
    }

    /**
     * 将匹配到的Ipv4地址的4个分组分别处理
     *
     * @param matcher 匹配到的Ipv4正则
     * @return ipv4对应long
     */
    private static long matchAddress(Matcher matcher) {
        long addr = 0;
        for (int i = 1; i <= 4; ++i) {
            addr |= Long.parseLong(matcher.group(i)) << 8 * (4 - i);
        }
        return addr;
    }

    /**
     * 检测本地端口可用性
     *
     * @param port 被检测的端口
     * @return 是否可用
     */
    public static boolean isUsableLocalPort(int port) {
        if (false == isValidPort(port)) {
            // 给定的IP未在指定端口范围中
            return false;
        }

        // 绑定非127.0.0.1的端口无法被检测到
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReuseAddress(true);
        } catch (IOException ignored) {
            return false;
        }

        try (DatagramSocket ds = new DatagramSocket(port)) {
            ds.setReuseAddress(true);
        } catch (IOException ignored) {
            return false;
        }

        return true;
    }

    /**
     * 是否为有效的端口
     * 此方法并不检查端口是否被占用
     *
     * @param port 端口号
     * @return 是否有效
     */
    public static boolean isValidPort(int port) {
        // 有效端口是0～65535
        return port >= 0 && port <= PORT_RANGE_MAX;
    }

    /**
     * 查找1024~65535范围内的可用端口
     * 此方法只检测给定范围内的随机一个端口,检测65535-1024次
     *
     * @return 可用的端口
     */
    public static int getUsableLocalPort() {
        return getUsableLocalPort(PORT_RANGE_MIN);
    }

    /**
     * 查找指定范围内的可用端口,最大值为65535
     * 此方法只检测给定范围内的随机一个端口,检测65535-minPort次
     *
     * @param minPort 端口最小值(包含)
     * @return 可用的端口
     */
    public static int getUsableLocalPort(int minPort) {
        return getUsableLocalPort(minPort, PORT_RANGE_MAX);
    }

    /**
     * 查找指定范围内的可用端口
     * 此方法只检测给定范围内的随机一个端口,检测maxPort-minPort次
     *
     * @param minPort 端口最小值(包含)
     * @param maxPort 端口最大值(包含)
     * @return 可用的端口
     */
    public static int getUsableLocalPort(int minPort, int maxPort) {
        final int maxPortExclude = maxPort + 1;
        int randomPort;
        for (int i = minPort; i < maxPortExclude; i++) {
            randomPort = RandomKit.randomInt(minPort, maxPortExclude);
            if (isUsableLocalPort(randomPort)) {
                return randomPort;
            }
        }

        throw new InstrumentException("Could not find an available port in the range [{}, {}] after {} attempts", minPort, maxPort, maxPort - minPort);
    }

    /**
     * 获得本机物理地址
     *
     * @return 本机物理地址
     */
    public static byte[] getLocalHardwareAddress() {
        return getHardwareAddress(getLocalhost());
    }

    /**
     * 获得指定地址信息中的硬件地址
     *
     * @param inetAddress {@link InetAddress}
     * @return 硬件地址
     */
    public static byte[] getHardwareAddress(InetAddress inetAddress) {
        if (null == inetAddress) {
            return null;
        }

        try {
            final NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            if (null != networkInterface) {
                return networkInterface.getHardwareAddress();
            }
        } catch (SocketException e) {
            throw new InstrumentException(e);
        }
        return null;
    }


    /**
     * 获取多个本地可用端口
     *
     * @param numRequested int
     * @param minPort      端口最小值(包含)
     * @param maxPort      端口最大值(包含)
     * @return 可用的端口
     */
    public static TreeSet<Integer> getUsableLocalPorts(int numRequested, int minPort, int maxPort) {
        final TreeSet<Integer> availablePorts = new TreeSet<>();
        int attemptCount = 0;
        while ((++attemptCount <= numRequested + 100) && availablePorts.size() < numRequested) {
            availablePorts.add(getUsableLocalPort(minPort, maxPort));
        }

        if (availablePorts.size() != numRequested) {
            throw new InstrumentException("Could not find {} available  ports in the range [{}, {}]", numRequested, minPort, maxPort);
        }
        return availablePorts;
    }

    /**
     * 判定是否为内网IP
     * 私有IP：A类 10.0.0.0-10.255.255.255 B类 172.16.2.2-172.31.255.255 C类 192.168.0.0-192.168.255.255 当然,还有127这个网段是环回地址
     *
     * @param ipAddress IP地址
     * @return 是否为内网IP
     */
    public static boolean isInnerIP(String ipAddress) {
        boolean isInnerIp;
        long ipNum = ipv4ToLong(ipAddress);

        long aBegin = ipv4ToLong("10.0.0.0");
        long aEnd = ipv4ToLong("10.255.255.255");

        long bBegin = ipv4ToLong("172.16.2.2");
        long bEnd = ipv4ToLong("172.31.255.255");

        long cBegin = ipv4ToLong("192.168.0.0");
        long cEnd = ipv4ToLong("192.168.255.255");

        isInnerIp = isInner(ipNum, aBegin, aEnd) || isInner(ipNum, bBegin, bEnd) || isInner(ipNum, cBegin, cEnd) || ipAddress.equals(Http.HTTP_HOST_IPV4);
        return isInnerIp;
    }

    /**
     * 相对URL转换为绝对URL
     *
     * @param absoluteBasePath 基准路径,绝对
     * @param relativePath     相对路径
     * @return 绝对URL
     */
    public static String toAbsoluteUrl(String absoluteBasePath, String relativePath) {
        try {
            URL absoluteUrl = new URL(absoluteBasePath);
            return new URL(absoluteUrl, relativePath).toString();
        } catch (Exception e) {
            throw new InstrumentException("To absolute url [{}] base [{}] error!", relativePath, absoluteBasePath);
        }
    }

    /**
     * 隐藏掉IP地址的最后一部分为 * 代替
     *
     * @param ip IP地址
     * @return 隐藏部分后的IP
     */
    public static String hideIpPart(String ip) {
        return new StringBuffer(ip.length()).append(ip, 0, ip.lastIndexOf(Symbol.DOT) + 1).append(Symbol.STAR).toString();
    }

    /**
     * 隐藏掉IP地址的最后一部分为 * 代替
     *
     * @param ip IP地址
     * @return 隐藏部分后的IP
     */
    public static String hideIpPart(long ip) {
        return hideIpPart(longToIpv4(ip));
    }

    /**
     * 构建InetSocketAddress
     * 当host中包含端口时(用“：”隔开),使用host中的端口,否则使用默认端口
     * 给定host为空时使用本地host(127.0.0.1)
     *
     * @param host        Host
     * @param defaultPort 默认端口
     * @return InetSocketAddress
     */
    public static InetSocketAddress buildInetSocketAddress(String host, int defaultPort) {
        if (StringKit.isBlank(host)) {
            host = Http.HTTP_HOST_IPV4;
        }

        String destHost;
        int port;
        int index = host.indexOf(Symbol.COLON);
        if (index != -1) {
            // host:port形式
            destHost = host.substring(0, index);
            port = Integer.parseInt(host.substring(index + 1));
        } else {
            destHost = host;
            port = defaultPort;
        }
        return new InetSocketAddress(destHost, port);
    }

    /**
     * 通过域名得到IP
     *
     * @param hostName HOST
     * @return ip address or hostName if UnknownHostException
     */
    public static String getIpByHost(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            return hostName;
        }
    }

    /**
     * 获取指定名称的网卡信息
     *
     * @param name 网络接口名，例如Linux下默认是eth0
     * @return 网卡，未找到返回<code>null</code>
     */
    public static NetworkInterface getNetworkInterface(String name) {
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }

        NetworkInterface netInterface;
        while (networkInterfaces.hasMoreElements()) {
            netInterface = networkInterfaces.nextElement();
            if (null != netInterface && name.equals(netInterface.getName())) {
                return netInterface;
            }
        }
        return null;
    }

    /**
     * 获取本机所有网卡
     *
     * @return 所有网卡, 异常返回<code>null</code>
     */
    public static Collection<NetworkInterface> getNetworkInterfaces() {
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }

        return CollKit.addAll(new ArrayList<>(), networkInterfaces);
    }

    /**
     * 获得本机的IPv4地址列表
     * 返回的IP列表有序,按照系统设备顺序
     *
     * @return IP地址列表 {@link LinkedHashSet}
     */
    public static LinkedHashSet<String> localIpv4s() {
        return toIpList(localAddressList(t -> t instanceof Inet4Address));
    }

    /**
     * 获得本机的IPv6地址列表
     * 返回的IP列表有序,按照系统设备顺序
     *
     * @return IP地址列表 {@link LinkedHashSet}
     */
    public static LinkedHashSet<String> localIpv6s() {
        return toIpList(localAddressList(t -> t instanceof Inet6Address));
    }

    /**
     * 地址列表转换为IP地址列表
     *
     * @param addressList 地址{@link Inet4Address} 列表
     * @return IP地址字符串列表
     */
    public static LinkedHashSet<String> toIpList(Set<InetAddress> addressList) {
        final LinkedHashSet<String> ipSet = new LinkedHashSet<>();
        for (InetAddress address : addressList) {
            ipSet.add(address.getHostAddress());
        }
        return ipSet;
    }

    /**
     * 获得本机的IP地址列表(包括Ipv4和Ipv6)
     * 返回的IP列表有序,按照系统设备顺序
     *
     * @return IP地址列表 {@link LinkedHashSet}
     */
    public static LinkedHashSet<String> localIps() {
        return toIpList(localAddressList(null));
    }

    /**
     * 获取所有满足过滤条件的本地IP地址对象
     *
     * @param addressFilter 过滤器,null表示不过滤,获取所有地址
     * @return 过滤后的地址对象列表
     */
    public static LinkedHashSet<InetAddress> localAddressList(Filter<InetAddress> addressFilter) {
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new InstrumentException(e);
        }

        if (null == networkInterfaces) {
            throw new InstrumentException("Get network interface error!");
        }

        final LinkedHashSet<InetAddress> ipSet = new LinkedHashSet<>();

        while (networkInterfaces.hasMoreElements()) {
            final NetworkInterface networkInterface = networkInterfaces.nextElement();
            final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                final InetAddress inetAddress = inetAddresses.nextElement();
                if (null != inetAddress && (null == addressFilter || addressFilter.accept(inetAddress))) {
                    ipSet.add(inetAddress);
                }
            }
        }

        return ipSet;
    }

    /**
     * 获取本机网卡IP地址,这个地址为所有网卡中非回路地址的第一个
     * 如果获取失败调用 {@link InetAddress#getLocalHost()}方法获取
     * 此方法不会抛出异常,获取失败将返回<code>null</code>
     * <p>
     * 参考：http://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
     *
     * @return 本机网卡IP地址, 获取失败返回<code>null</code>
     */
    public static String getLocalhostStr() {
        InetAddress localhost = getLocalhost();
        if (null != localhost) {
            return localhost.getHostAddress();
        }
        return null;
    }

    /**
     * 获取本机网卡IP地址,规则如下：
     *
     * <pre>
     * 1. 查找所有网卡地址,必须非回路(loopback)地址、非局域网地址(siteLocal)、IPv4地址
     * 2. 如果无满足要求的地址,调用 {@link InetAddress#getLocalHost()} 获取地址
     * </pre>
     * <p>
     * 此方法不会抛出异常,获取失败将返回<code>null</code>
     *
     * @return 本机网卡IP地址, 获取失败返回<code>null</code>
     */
    public static InetAddress getLocalhost() {
        final LinkedHashSet<InetAddress> localAddressList = localAddressList(address -> {
            // 非loopback地址，指127.*.*.*的地址
            return false == address.isLoopbackAddress()
                    // 需为IPV4地址
                    && address instanceof Inet4Address;
        });

        if (CollKit.isNotEmpty(localAddressList)) {
            InetAddress address2 = null;
            for (InetAddress inetAddress : localAddressList) {
                if (false == inetAddress.isSiteLocalAddress()) {
                    // 非地区本地地址，指10.0.0.0 ~ 10.255.255.255、172.16.0.0 ~ 172.31.255.255、192.168.0.0 ~ 192.168.255.255
                    return inetAddress;
                } else if (null == address2) {
                    address2 = inetAddress;
                }
            }

            if (null != address2) {
                return address2;
            }
        }

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // ignore
        }
        return null;
    }

    /**
     * 获取主机名称，一次获取会缓存名称
     *
     * @return 主机名称
     */
    public static String getLocalHostName() {
        final InetAddress localhost = getLocalhost();
        if (null != localhost) {
            String name = localhost.getHostName();
            if (StringKit.isEmpty(name)) {
                return localhost.getHostAddress();
            }
            return name;
        }
        return Normal.EMPTY;
    }

    /**
     * 获得本机MAC地址
     *
     * @return 本机MAC地址
     */
    public static String getLocalMacAddress() {
        return getMacAddress(getLocalhost());
    }

    /**
     * 获得指定地址信息中的MAC地址,使用分隔符“-”
     *
     * @param inetAddress {@link InetAddress}
     * @return MAC地址, 用-分隔
     */
    public static String getMacAddress(InetAddress inetAddress) {
        return getMacAddress(inetAddress, Symbol.MINUS);
    }

    /**
     * 获得指定地址信息中的MAC地址
     *
     * @param inetAddress {@link InetAddress}
     * @param separator   分隔符,推荐使用“-”或者“:”
     * @return MAC地址, 用-分隔
     */
    public static String getMacAddress(InetAddress inetAddress, String separator) {
        if (null == inetAddress) {
            return null;
        }

        byte[] mac = null;
        try {
            final NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            if (null != networkInterface) {
                mac = networkInterface.getHardwareAddress();
            }
        } catch (SocketException e) {
            throw new InstrumentException(e);
        }
        if (null != mac) {
            final StringBuilder sb = new StringBuilder();
            String s;
            for (int i = 0; i < mac.length; i++) {
                if (i != 0) {
                    sb.append(separator);
                }
                // 字节转换为整数
                s = Integer.toHexString(mac[i] & 0xFF);
                sb.append(s.length() == 1 ? 0 + s : s);
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * 创建 {@link InetSocketAddress}
     *
     * @param host 域名或IP地址,空表示任意地址
     * @param port 端口,0表示系统分配临时端口
     * @return {@link InetSocketAddress}
     */
    public static InetSocketAddress createAddress(String host, int port) {
        if (StringKit.isBlank(host)) {
            return new InetSocketAddress(port);
        }
        return new InetSocketAddress(host, port);
    }

    /**
     * 简易的使用Socket发送数据
     *
     * @param host    Server主机
     * @param port    Server端口
     * @param isBlock 是否阻塞方式
     * @param data    需要发送的数据
     * @throws InstrumentException IO异常
     */
    public static void netCat(String host, int port, boolean isBlock, ByteBuffer data) throws InstrumentException {
        try (SocketChannel channel = SocketChannel.open(createAddress(host, port))) {
            channel.configureBlocking(isBlock);
            channel.write(data);
        } catch (IOException e) {
            throw new InstrumentException(e);
        }
    }

    /**
     * 使用普通Socket发送数据
     *
     * @param host Server主机
     * @param port Server端口
     * @param data 数据
     * @throws InstrumentException 异常
     */
    public static void netCat(String host, int port, byte[] data) throws InstrumentException {
        OutputStream out = null;
        try (Socket socket = new Socket(host, port)) {
            out = socket.getOutputStream();
            out.write(data);
            out.flush();
        } catch (IOException e) {
            throw new InstrumentException(e);
        } finally {
            IoKit.close(out);
        }
    }

    /**
     * 是否在CIDR规则配置范围内
     * 方法来自：【成都】小邓
     *
     * @param ip   需要验证的IP
     * @param cidr CIDR规则
     * @return 是否在范围内
     */
    public static boolean isInRange(String ip, String cidr) {
        final int maskSplitMarkIndex = cidr.lastIndexOf(Symbol.SLASH);
        if (maskSplitMarkIndex < 0) {
            throw new IllegalArgumentException("Invalid cidr: " + cidr);
        }

        final long mask = (-1L << Normal._32 - Integer.parseInt(cidr.substring(maskSplitMarkIndex + 1)));
        long cidrIpAddr = ipv4ToLong(cidr.substring(0, maskSplitMarkIndex));

        return (ipv4ToLong(ip) & mask) == (cidrIpAddr & mask);
    }

    /**
     * Unicode域名转puny code
     *
     * @param unicode Unicode域名
     * @return puny code
     */
    public static String idnToASCII(String unicode) {
        return IDN.toASCII(unicode);
    }

    /**
     * 从多级反向代理中获得第一个非unknown IP地址
     *
     * @param ip 获得的IP地址
     * @return 第一个非unknown IP地址
     */
    public static String getMultistageReverseProxyIp(String ip) {
        // 多级反向代理检测
        if (null != ip && ip.indexOf(Symbol.COMMA) > 0) {
            final String[] ips = ip.trim().split(Symbol.COMMA);
            for (String subIp : ips) {
                if (false == isUnknown(subIp)) {
                    ip = subIp;
                    break;
                }
            }
        }
        return ip;
    }

    /**
     * 检测给定字符串是否为未知，多用于检测HTTP请求相关
     *
     * @param checkString 被检测的字符串
     * @return 是否未知
     */
    public static boolean isUnknown(String checkString) {
        return StringKit.isBlank(checkString) || "unknown".equalsIgnoreCase(checkString);
    }

    /**
     * 检测IP地址是否能ping通
     *
     * @param ip IP地址
     * @return the true/false 返回是否ping通
     */
    public static boolean ping(String ip) {
        return ping(ip, 200);
    }

    /**
     * 检测IP地址是否能ping通
     *
     * @param ip      IP地址
     * @param timeout 检测超时(毫秒)
     * @return the true/false 是否ping通
     */
    public static boolean ping(String ip, int timeout) {
        try {
            return InetAddress.getByName(ip).isReachable(timeout);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 解析Cookie信息
     *
     * @param cookieStr Cookie字符串
     * @return cookie字符串
     */
    public static List<HttpCookie> parseCookies(String cookieStr) {
        if (StringKit.isBlank(cookieStr)) {
            return Collections.emptyList();
        }
        return HttpCookie.parse(cookieStr);
    }

    /**
     * 检查远程端口是否开启
     *
     * @param address 远程地址
     * @param timeout 检测超时
     * @return 远程端口是否开启
     */
    public static boolean isOpen(InetSocketAddress address, int timeout) {
        try (Socket sc = new Socket()) {
            sc.connect(address, timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取DNS信息，如TXT信息：
     *
     * <pre class="code">
     *     NetKit.attrNames("aoju.org", "TXT")
     * </pre>
     *
     * @param hostName  主机域名
     * @param attrNames 属性
     * @return DNS信息
     */
    public static List<String> getDnsInfo(String hostName, String... attrNames) {
        final String uri = StringKit.addPrefixIfNot(hostName, "dns:");
        final Attributes attributes = getAttributes(uri, attrNames);

        final List<String> infos = new ArrayList<>();
        for (Attribute attribute : new EnumerationIterator<>(attributes.getAll())) {
            try {
                infos.add((String) attribute.get());
            } catch (NamingException ignore) {
                //ignore
            }
        }
        return infos;
    }

    /**
     * 创建{@link InitialDirContext}
     *
     * @param environment 环境参数，{code null}表示无参数
     * @return {@link InitialDirContext}
     */
    public static InitialDirContext createInitialDirContext(Map<String, String> environment) {
        try {
            if (MapKit.isEmpty(environment)) {
                return new InitialDirContext();
            }
            return new InitialDirContext(Convert.convert(Hashtable.class, environment));
        } catch (NamingException e) {
            throw new InstrumentException(e);
        }
    }

    /**
     * 创建{@link InitialContext}
     *
     * @param environment 环境参数，{code null}表示无参数
     * @return {@link InitialContext}
     */
    public static InitialContext createInitialContext(Map<String, String> environment) {
        try {
            if (MapKit.isEmpty(environment)) {
                return new InitialContext();
            }
            return new InitialContext(Convert.convert(Hashtable.class, environment));
        } catch (NamingException e) {
            throw new InstrumentException(e);
        }
    }

    /**
     * 获取指定容器环境的对象的属性<br>
     * 如获取DNS属性，则URI为类似：dns:aoju.cn
     *
     * @param uri     URI字符串，格式为[scheme:][name]/[domain]
     * @param attrIds 需要获取的属性ID名称
     * @return {@link Attributes}
     */
    public static Attributes getAttributes(String uri, String... attrIds) {
        try {
            return createInitialDirContext(null).getAttributes(uri, attrIds);
        } catch (NamingException e) {
            throw new InstrumentException(e);
        }
    }

    /**
     * 指定IP的long是否在指定范围内
     *
     * @param userIp 用户IP
     * @param begin  开始IP
     * @param end    结束IP
     * @return 是否在范围内
     */
    private static boolean isInner(long userIp, long begin, long end) {
        return (userIp >= begin) && (userIp <= end);
    }

}
