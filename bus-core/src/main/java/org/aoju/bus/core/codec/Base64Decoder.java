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
package org.aoju.bus.core.codec;

import org.aoju.bus.core.lang.Charset;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.toolkit.ArrayKit;
import org.aoju.bus.core.toolkit.StringKit;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Base64解码实现
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public class Base64Decoder {

    private static final byte PADDING = -2;

    /**
     * base64解码
     *
     * @param source 被解码的base64字符串
     * @return 被加密后的字符串
     */
    public static String decodeStr(CharSequence source) {
        return decodeStr(source, Charset.UTF_8);
    }

    /**
     * base64解码
     *
     * @param source  被解码的base64字符串
     * @param charset 字符集
     * @return 被加密后的字符串
     */
    public static String decodeStr(CharSequence source, java.nio.charset.Charset charset) {
        return StringKit.toString(decode(source), charset);
    }

    /**
     * base64解码
     *
     * @param source 被解码的base64字符串
     * @return 被加密后的字符串
     */
    public static byte[] decode(CharSequence source) {
        return decode(StringKit.bytes(source, Charset.UTF_8));
    }

    /**
     * 解码Base64
     *
     * @param in 输入
     * @return 解码后的bytes
     */
    public static byte[] decode(byte[] in) {
        if (ArrayKit.isEmpty(in)) {
            return in;
        }
        return decode(in, 0, in.length);
    }

    /**
     * 解码Base64
     *
     * @param in     输入
     * @param pos    开始位置
     * @param length 长度
     * @return 解码后的bytes
     */
    public static byte[] decode(byte[] in, int pos, int length) {
        if (ArrayKit.isEmpty(in)) {
            return in;
        }

        final IntWrapper offset = new IntWrapper(pos);

        byte sestet0;
        byte sestet1;
        byte sestet2;
        byte sestet3;
        int maxPos = pos + length - 1;
        int octetId = 0;
        byte[] octet = new byte[length * 3 / 4];// over-estimated if non-base64 characters present
        while (offset.value <= maxPos) {
            sestet0 = getNextValidDecodeByte(in, offset, maxPos);
            sestet1 = getNextValidDecodeByte(in, offset, maxPos);
            sestet2 = getNextValidDecodeByte(in, offset, maxPos);
            sestet3 = getNextValidDecodeByte(in, offset, maxPos);

            if (PADDING != sestet1) {
                octet[octetId++] = (byte) ((sestet0 << 2) | (sestet1 >>> 4));
            }
            if (PADDING != sestet2) {
                octet[octetId++] = (byte) (((sestet1 & 0xf) << 4) | (sestet2 >>> 2));
            }
            if (PADDING != sestet3) {
                octet[octetId++] = (byte) (((sestet2 & 3) << 6) | sestet3);
            }
        }

        if (octetId == octet.length) {
            return octet;
        } else {
            // 如果有非Base64字符混入，则实际结果比解析的要短，截取之
            return (byte[]) ArrayKit.copy(octet, new byte[octetId], octetId);
        }
    }

    /**
     * 解码Base64
     *
     * @param ch  字符信息
     * @param off 结束为止
     * @param len 长度
     * @param out 输出流
     * @throws IOException 异常信息
     */
    public static void decode(char[] ch, int off, int len, OutputStream out)
            throws IOException {
        byte b2, b3;
        while ((len -= 2) >= 0) {
            out.write((byte) ((Normal.DECODE_64_TABLE[ch[off++]] << 2)
                    | ((b2 = Normal.DECODE_64_TABLE[ch[off++]]) >>> 4)));
            if ((len-- == 0) || ch[off] == Symbol.C_EQUAL)
                break;
            out.write((byte) ((b2 << 4)
                    | ((b3 = Normal.DECODE_64_TABLE[ch[off++]]) >>> 2)));
            if ((len-- == 0) || ch[off] == Symbol.C_EQUAL)
                break;
            out.write((byte) ((b3 << 6) | Normal.DECODE_64_TABLE[ch[off++]]));
        }
    }

    /**
     * 给定的字符是否为Base64字符
     *
     * @param octet 被检查的字符
     * @return 是否为Base64字符
     */
    public static boolean isBase64Code(byte octet) {
        return octet == Symbol.C_EQUAL || (octet >= 0 && octet < Normal.DECODE_64_TABLE.length && Normal.DECODE_64_TABLE[octet] != -1);
    }

    /**
     * 获取下一个有效的byte字符
     *
     * @param in     输入
     * @param pos    当前位置，调用此方法后此位置保持在有效字符的下一个位置
     * @param maxPos 最大位置
     * @return 有效字符，如果达到末尾返回
     */
    private static byte getNextValidDecodeByte(byte[] in, IntWrapper pos, int maxPos) {
        byte base64Byte;
        byte decodeByte;
        while (pos.value <= maxPos) {
            base64Byte = in[pos.value++];
            if (base64Byte > -1) {
                decodeByte = Normal.DECODE_64_TABLE[base64Byte];
                if (decodeByte > -1) {
                    return decodeByte;
                }
            }
        }
        return PADDING;
    }

    /**
     * int包装,使之可变
     */
    private static class IntWrapper {
        int value;

        IntWrapper(int value) {
            this.value = value;
        }
    }

}
