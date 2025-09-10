package com.gengzi.sftp.nio;


import com.gengzi.sftp.nio.util.S3Util;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Set;

/**
 * 文件数据流通道
 *
 * @author gengzi
 */
public class S3SftpSeekableByteChannel implements SeekableByteChannel {


    public static final long TIMEOUT_TIME_LENGTH_1 = 1L;
    // 定义一个s3的工具类
    private final S3Util s3Util;
    // 定义position 当前通道读写位置
    private long position;
    // 定义一个close 标志，表示当前文件通道是否已经关闭
    private boolean close;
    // 定义一个文件大小的size 字节数
    private long size = 0L;
    // 局部变量
    private S3SftpPath path;
    // 定义一个读通道
    private S3SftpReadableByteChannel readableByteChannel;
    // 定义一个写通道
    private S3SftpWritableByteChannel writableByteChannel;

    public S3SftpReadableByteChannel getReadableByteChannel() {
        return readableByteChannel;
    }

    public S3SftpWritableByteChannel getWritableByteChannel() {
        return writableByteChannel;
    }

    // 包含了一个读通道和写通道
    public S3SftpSeekableByteChannel(S3SftpPath s3Path, S3AsyncClient s3Client, Set<? extends OpenOption> options) throws IOException {
        this.size = -1L;
        this.s3Util = new S3Util(s3Client, null, null);
        // 初始化position
        this.position = 0L;
        // 初始化
        this.path = s3Path;
        //TODO 根据不同模式判断是创建读通道还是写通道 获取读通道，用于向s3存储读取内容\

        // 读和写不能同时进行
        if (options.contains(StandardOpenOption.READ) && options.contains(StandardOpenOption.WRITE)) {
            throw new IOException("This channel does not support read and write access simultaneously");
        }
        // SYNC：要求每次写操作都同步到底层存储设备（保证数据持久化，性能较低）。
        //DSYNC：类似 SYNC，但仅同步数据（不保证文件元数据同步）。
        if (options.contains(StandardOpenOption.SYNC) || options.contains(StandardOpenOption.DSYNC)) {
            throw new IOException("The SYNC/DSYNC options is not supported");
        }


        if (options.contains(StandardOpenOption.READ)) {
            S3SftpNioSpiConfiguration configuration = path.getFileSystem().configuration();
            this.readableByteChannel = new S3SftpReadableByteChannel(s3Path, 5, 10,
                    s3Client, this, null, null);
            this.writableByteChannel = null;
        } else if (options.contains(StandardOpenOption.WRITE)) {
            this.readableByteChannel = null;
            this.writableByteChannel = new S3SftpWritableByteChannel(s3Path, s3Client, options, s3Util);
        }

        // 初始化close为false未关闭
        this.close = false;

    }

    /**
     * 这段代码是Java中ReadableByteChannel接口的read方法注释文档。
     * 功能解释：
     * 从当前通道的位置开始读取字节序列到指定的缓冲区(dst)
     * 读取完成后会更新通道的当前位置，增加实际读取的字节数
     * 方法行为与ReadableByteChannel接口规范完全一致
     * 这是一个标准的字节通道读取操作，用于将数据从通道传输到字节缓冲区中
     * <p>
     * <p>
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p> Bytes are read starting at this channel's current position, and
     * then the position is updated with the number of bytes actually read.
     * Otherwise this method behaves exactly as specified in the {@link
     * ReadableByteChannel} interface.
     *
     * @param dst
     */
    @Override
    public int read(ByteBuffer dst) throws IOException {
        validateOpen();
        if (readableByteChannel == null) {
            throw new NonReadableChannelException();

        }
        return readableByteChannel.read(dst);
    }

    /**
     * 这段代码是Java NIO中WritableByteChannel接口的write方法注释，描述了向通道写入字节缓冲区的功能：
     * 从缓冲区向通道写入字节序列
     * 写入位置从通道当前position开始
     * 若通道连接的实体（如文件）以APPEND模式打开，则先将position移到末尾
     * 写入时会根据需要扩展连接的实体大小
     * 最后更新position为实际写入的字节数
     * <p>
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p> Bytes are written starting at this channel's current position, unless
     * the channel is connected to an entity such as a file that is opened with
     * the {@link StandardOpenOption#APPEND APPEND} option, in
     * which case the position is first advanced to the end. The entity to which
     * the channel is connected is grown, if necessary, to accommodate the
     * written bytes, and then the position is updated with the number of bytes
     * actually written. Otherwise this method behaves exactly as specified by
     * the {@link WritableByteChannel} interface.
     *
     * @param src
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        validateOpen();
        if (writableByteChannel == null) {
            throw new NonWritableChannelException();
        }
        int remaining = src.remaining();
        this.position += remaining;
        int write = writableByteChannel.write(src);
        return write;
    }

    /**
     * Returns this channel's position.
     *
     * @return This channel's position,
     * a non-negative integer counting the number of bytes
     * from the beginning of the entity to the current position
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    @Override
    public long position() throws IOException {
        //  获取当前通道的读写位置
        // 加锁
        // 判断当前文件通道是否已经关闭
        validateOpen();

        synchronized (this) {
            return position;
        }
    }

    private void validateOpen() throws ClosedChannelException {
        if (this.close) {
            throw new ClosedChannelException();
        }
    }


    /**
     * 这段代码是Java NIO中用于设置通道位置的方法注释。主要功能是：
     * 将通道的当前位置设置为指定的新位置
     * 如果新位置超出当前文件大小，不会改变文件大小
     * 读取时会立即返回文件结束标识
     * 写入时会扩展文件大小
     * 不建议在追加模式下设置位置，因为写入前会自动定位到文件末尾
     * Sets this channel's position.
     *
     * <p> Setting the position to a value that is greater than the current size
     * is legal but does not change the size of the entity.  A later attempt to
     * read bytes at such a position will immediately return an end-of-file
     * indication.  A later attempt to write bytes at such a position will cause
     * the entity to grow to accommodate the new bytes; the values of any bytes
     * between the previous end-of-file and the newly-written bytes are
     * unspecified.
     *
     * <p> Setting the channel's position is not recommended when connected to
     * an entity, typically a file, that is opened with the {@link
     * StandardOpenOption#APPEND APPEND} option. When opened for
     * append, the position is first advanced to the end before writing.
     *
     * @param newPosition The new position, a non-negative integer counting
     *                    the number of bytes from the beginning of the entity
     * @return This channel
     * @throws ClosedChannelException   If this channel is closed
     * @throws IllegalArgumentException If the new position is negative
     * @throws IOException              If some other I/O error occurs
     */
    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {


        if (newPosition < 0) {
            throw new IllegalArgumentException("newPosition < 0");
        }

        if (!isOpen()) {
            throw new ClosedChannelException();
        }

        //TODO


        // 将通道位置设置为新位置
        synchronized (this) {
            this.position = newPosition;
            return this;
        }


    }

    /**
     * 这段代码是Java NIO中FileChannel类的size()方法的文档注释。
     * 功能： 获取与此通道连接的实体（文件）的当前大小，以字节为单位返回。
     * 异常情况：
     * 如果通道已关闭，抛出ClosedChannelException
     * 如果发生其他I/O错误，抛出IOException
     * 该方法用于查询文件的大小信息
     * <p>
     * Returns the current size of entity to which this channel is connected.
     *
     * @return The current size, measured in bytes
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    @Override
    public long size() throws IOException {
        // 需要从对象存储中获取对应的文件大小
        validateOpen();
        if (this.size < 0) {
            this.size = S3SftpBasicFileAttributes.get(path, Duration.ofMinutes(TIMEOUT_TIME_LENGTH_1)).size();
            return size;
        }

        return this.size;
    }

    /**
     * 这段代码是Java NIO中FileChannel的truncate方法文档注释。
     * 功能： 将连接到此通道的实体（通常是文件）截断到指定大小。
     * 逻辑：
     * 如果指定大小小于当前大小，截断文件并丢弃超出部分
     * 如果指定大小大于等于当前大小，文件不变
     * 如果当前position大于指定大小，将position设置为指定大小
     * 限制： 使用APPEND模式打开的文件可能禁止截断操作。
     * 异常： 不可写通道、已关闭通道、负数大小等情况会抛出相应异常。
     * Truncates the entity, to which this channel is connected, to the given
     * size.
     *
     * <p> If the given size is less than the current size then the entity is
     * truncated, discarding any bytes beyond the new end. If the given size is
     * greater than or equal to the current size then the entity is not modified.
     * In either case, if the current position is greater than the given size
     * then it is set to that size.
     *
     * <p> An implementation of this interface may prohibit truncation when
     * connected to an entity, typically a file, opened with the {@link
     * StandardOpenOption#APPEND APPEND} option.
     *
     * @param size The new size, a non-negative byte count
     * @return This channel
     * @throws NonWritableChannelException If this channel was not opened for writing
     * @throws ClosedChannelException      If this channel is closed
     * @throws IllegalArgumentException    If the new size is negative
     * @throws IOException                 If some other I/O error occurs
     */
    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new UnsupportedOperationException("Currently not supported");
    }

    /**
     * Tells whether or not this channel is open.
     *
     * @return {@code true} if, and only if, this channel is open
     */
    @Override
    public boolean isOpen() {
        synchronized (this) {
            return !close;
        }
    }

    /**
     * Closes this channel.
     *
     * <p> After a channel is closed, any further attempt to invoke I/O
     * operations upon it will cause a {@link ClosedChannelException} to be
     * thrown.
     *
     * <p> If this channel is already closed then invoking this method has no
     * effect.
     *
     * <p> This method may be invoked at any time.  If some other thread has
     * already invoked it, however, then another invocation will block until
     * the first invocation is complete, after which it will return without
     * effect. </p>
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        //TODO 从 channel 集合中移除


        synchronized (this) {
            if (this.readableByteChannel != null) {
                this.readableByteChannel.close();
            }
            if (this.writableByteChannel != null) {
                this.writableByteChannel.close();
            }

            this.close = true;
        }
    }
}
