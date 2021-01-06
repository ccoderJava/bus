/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org and other contributors.                      *
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
package org.aoju.bus.core.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 包装当前会话分配到的虚拟Buffer,提供流式操作方式
 *
 * @author Kimi Liu
 * @version 6.1.8
 * @since JDK 1.8+
 */
public final class WriteBuffer extends OutputStream {

    /**
     * 存储已就绪待输出的数据
     */
    private final VirtualBuffer[] items;
    /**
     * 同步锁
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * Condition for waiting puts
     */
    private final Condition notFull = lock.newCondition();
    /**
     * 当缓冲队列已满时，触发线程阻塞条件
     */
    private final Condition waiting = lock.newCondition();
    /**
     * 为当前 WriteBuffer 提供数据存放功能的缓存页
     */
    private final PageBuffer pageBuffer;
    /**
     * 缓冲区数据刷新Function
     */
    private final Consumer<WriteBuffer> consumer;
    /**
     * 默认内存块大小
     */
    private final int chunkSize;
    /**
     * 当时是否符合wait条件
     */
    private volatile boolean isWaiting = false;
    /**
     * items 读索引位
     */
    private int takeIndex;
    /**
     * items 写索引位
     */
    private int putIndex;
    /**
     * items 中存放的缓冲数据数量
     */
    private int count;
    /**
     * 暂存当前业务正在输出的数据,输出完毕后会存放到items中
     */
    private VirtualBuffer writeInBuf;
    /**
     * 当前WriteBuffer是否已关闭
     */
    private boolean closed = false;
    /**
     * 辅助8字节以内输出的缓存组数
     */
    private byte[] cacheByte;

    public WriteBuffer(PageBuffer pageBuffer, Consumer<WriteBuffer> consumer, int chunkSize, int capacity) {
        this.pageBuffer = pageBuffer;
        this.consumer = consumer;
        this.items = new VirtualBuffer[capacity];
        this.chunkSize = chunkSize;
    }

    /**
     * 按照{@link OutputStream#write(int)}规范：要写入的字节是参数 b 的八个低位。 b 的 24 个高位将被忽略。
     * 而使用该接口时容易传入非byte范围内的数据，接口定义与实际使用出现歧义的可能性较大，故建议废弃该方法，选用{@link WriteBuffer#writeByte(byte)}。
     *
     * @param b 输出字节
     */
    @Override
    public void write(int b) {
        writeByte((byte) b);
    }

    /**
     * 输出一个short类型的数据
     *
     * @param v short数值
     * @throws IOException IO异常
     */
    public void writeShort(short v) throws IOException {
        initCacheBytes();
        cacheByte[0] = (byte) ((v >>> 8) & 0xFF);
        cacheByte[1] = (byte) (v & 0xFF);
        write(cacheByte, 0, 2);
    }

    /**
     * @param b 待输出数值
     * @see #write(int)
     */
    public void writeByte(byte b) {
        lock.lock();
        try {
            if (writeInBuf == null) {
                writeInBuf = pageBuffer.allocate(chunkSize);
            }
            writeInBuf.buffer().put(b);
            flushWriteBuffer(false);
        } finally {
            lock.unlock();
        }

        consumer.accept(this);
    }

    private void flushWriteBuffer(boolean forceFlush) {
        if (!forceFlush && writeInBuf.buffer().hasRemaining()) {
            return;
        }
        consumer.accept(this);
        if (writeInBuf != null) {
            writeInBuf.buffer().flip();
            VirtualBuffer buffer = writeInBuf;
            writeInBuf = null;
            this.put(buffer);
        }
    }

    /**
     * 输出int数值,占用4个字节
     *
     * @param v int数值
     * @throws IOException IO异常
     */
    public void writeInt(int v) throws IOException {
        initCacheBytes();
        cacheByte[0] = (byte) ((v >>> 24) & 0xFF);
        cacheByte[1] = (byte) ((v >>> 16) & 0xFF);
        cacheByte[2] = (byte) ((v >>> 8) & 0xFF);
        cacheByte[3] = (byte) (v & 0xFF);
        write(cacheByte, 0, 4);
    }


    /**
     * 输出long数值,占用8个字节
     *
     * @param v long数值
     * @throws IOException IO异常
     */
    public void writeLong(long v) throws IOException {
        initCacheBytes();
        cacheByte[0] = (byte) ((v >>> 56) & 0xFF);
        cacheByte[1] = (byte) ((v >>> 48) & 0xFF);
        cacheByte[2] = (byte) ((v >>> 40) & 0xFF);
        cacheByte[3] = (byte) ((v >>> 32) & 0xFF);
        cacheByte[4] = (byte) ((v >>> 24) & 0xFF);
        cacheByte[5] = (byte) ((v >>> 16) & 0xFF);
        cacheByte[6] = (byte) ((v >>> 8) & 0xFF);
        cacheByte[7] = (byte) (v & 0xFF);
        write(cacheByte, 0, 8);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            waitPreWriteFinish();
            do {
                if (writeInBuf == null) {
                    writeInBuf = pageBuffer.allocate(Math.max(chunkSize, len));
                }
                ByteBuffer writeBuffer = writeInBuf.buffer();
                if (closed) {
                    writeInBuf.clean();
                    throw new IOException("writeBuffer has closed");
                }
                int minSize = Math.min(writeBuffer.remaining(), len);
                writeBuffer.put(b, off, minSize);
                off += minSize;
                len -= minSize;
                flushWriteBuffer(false);
            } while (len > 0);
            notifyWaiting();
        } finally {
            lock.unlock();
        }
    }

    public void write(ByteBuffer buffer) throws IOException {
        write(VirtualBuffer.wrap(buffer));
    }

    public void write(VirtualBuffer virtualBuffer) throws IOException {
        lock.lock();
        try {
            waitPreWriteFinish();
            if (writeInBuf != null && !virtualBuffer.buffer().isDirect()
                    && writeInBuf.buffer().remaining() > virtualBuffer.buffer().remaining()) {
                writeInBuf.buffer().put(virtualBuffer.buffer());
                virtualBuffer.clean();
            } else {
                if (writeInBuf != null) {
                    flushWriteBuffer(true);
                }
                virtualBuffer.buffer().compact();
                writeInBuf = virtualBuffer;
            }
            flushWriteBuffer(false);
            notifyWaiting();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 唤醒处于waiting状态的线程
     */
    private void notifyWaiting() {
        isWaiting = false;
        waiting.signal();
    }

    /**
     * 初始化8字节的缓存数值
     */
    private void initCacheBytes() {
        if (cacheByte == null) {
            cacheByte = new byte[8];
        }
    }

    /**
     * 确保数据输出有序性
     *
     * @throws IOException 如果发生 I/O 错误
     */
    private void waitPreWriteFinish() throws IOException {
        while (isWaiting) {
            try {
                waiting.await();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * 写入内容并刷新缓冲区
     *
     * @param b 待输出数据
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeAndFlush(byte[] b) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        writeAndFlush(b, 0, b.length);
    }

    /**
     * @param b   待输出数据
     * @param off b的起始位点
     * @param len 从b中输出的数据长度
     * @throws IOException 如果发生 I/O 错误
     * @see WriteBuffer#writeAndFlush(byte[])
     */
    public void writeAndFlush(byte[] b, int off, int len) throws IOException {
        write(b, off, len);
        flush();
    }

    @Override
    public void flush() {
        if (closed) {
            throw new RuntimeException("OutputStream has closed");
        }
        if (this.count > 0 || writeInBuf != null) {
            consumer.accept(this);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        lock.lock();
        try {
            flush();
            closed = true;
            VirtualBuffer byteBuf;
            while ((byteBuf = poll()) != null) {
                byteBuf.clean();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 是否存在待输出的数据
     *
     * @return true:有,false:无
     */
    public boolean hasData() {
        return count > 0 || writeInBuf != null;
    }

    /**
     * 存储缓冲区至队列中以备输出
     *
     * @param virtualBuffer 缓存对象
     */
    public void put(VirtualBuffer virtualBuffer) {
        try {
            while (count == items.length) {
                isWaiting = true;
                notFull.await();
                //防止因close诱发内存泄露
                if (closed) {
                    virtualBuffer.clean();
                    return;
                }
            }

            items[putIndex] = virtualBuffer;
            if (++putIndex == items.length) {
                putIndex = 0;
            }
            count++;
        } catch (InterruptedException e1) {
            throw new RuntimeException(e1);
        }
    }

    /**
     * 获取并移除当前缓冲队列中头部的VirtualBuffer
     *
     * @return 待输出的VirtualBuffer
     */
    public VirtualBuffer poll() {
        if (count == 0 && writeInBuf == null) {
            return null;
        }
        lock.lock();
        try {
            if (count == 0) {
                if (writeInBuf != null) {
                    writeInBuf.buffer().flip();
                    VirtualBuffer buffer = writeInBuf;
                    writeInBuf = null;
                    return buffer;
                } else {
                    return null;
                }
            }

            VirtualBuffer x = items[takeIndex];
            items[takeIndex] = null;
            if (++takeIndex == items.length) {
                takeIndex = 0;
            }
            if (count-- == items.length) {
                notFull.signal();
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

}