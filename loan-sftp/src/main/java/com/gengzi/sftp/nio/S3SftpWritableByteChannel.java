package com.gengzi.sftp.nio;

import com.gengzi.sftp.nio.util.S3Util;
import org.checkerframework.checker.nullness.qual.NonNull;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


/**
 * S3sftp写通道
 */
public class S3SftpWritableByteChannel implements WritableByteChannel {


    // 定义一个本地文件操作通道
    private final SeekableByteChannel channel;
    // 定义一个临时文件
    private final Path tempFile;
    // 定义当前通道是否打开
    private boolean isOpen = false;


    private final S3Util s3Util;

    private final S3SftpPath s3SftpPath;



    public S3SftpWritableByteChannel(S3SftpPath s3SftpPath, S3AsyncClient s3Client,
                                     Set<? extends OpenOption> options, S3Util s3Util) throws IOException {

        this.s3Util = s3Util;
        this.s3SftpPath = s3SftpPath;
        // 判断当前文件是否存在
        S3SftpFileSystemProvider provider = (S3SftpFileSystemProvider) s3SftpPath.getFileSystem().provider();
        Boolean exists = provider.exists(s3Client, s3SftpPath);
        // 存在该文件，并且模式为 CREATE_NEW：若文件不存在则创建；若已存在则抛出 FileAlreadyExistsException（严格避免覆盖）。
        if (exists && options.contains(StandardOpenOption.CREATE_NEW)) {
            // 文件已经存在
            throw new FileAlreadyExistsException("file already exists");
        }
        // 判断现在写入的模式
        if (!exists && !options.contains(StandardOpenOption.CREATE_NEW) && !options.contains(StandardOpenOption.CREATE)) {
            // 如果文件不存在，写入模式也不是创建
            throw new NoSuchFileException("File at path:" + s3SftpPath + " does not exist yet");
        }

        //TODO 生成临时文件的名称？？？ 可自定义？
        this.tempFile = Files.createTempFile("s3-sftp-tmp", ".tmp");
        if (exists) {
            // 下载文件到本地临时
            s3Util.downloadToLocalFile(s3SftpPath, tempFile);
        }
        channel = Files.newByteChannel(this.tempFile, removeCreateNew(options));


        this.isOpen = true;
    }

    private @NonNull Set<? extends OpenOption> removeCreateNew(Set<? extends OpenOption> options) {
        var auxOptions = new HashSet<>(options);
        auxOptions.remove(StandardOpenOption.CREATE_NEW);
        return Set.copyOf(auxOptions);
    }

    /**
     * WritableByteChannel接口的write方法的文档注释，功能是将指定缓冲区（src）中的字节序列写入通道。具体说明如下：
     * 尝试写入缓冲区中剩余的所有字节（src.remaining()）。
     * 实际写入的字节数可能小于等于尝试写入的数量。
     * 写入从缓冲区当前位置开始，写入后位置前移相应字节数。
     * 方法可能阻塞，直到写入完成或通道关闭。
     * 支持多线程调用，但并发写入会阻塞等待。
     * 该方法返回实际写入的字节数，可能抛出多种I/O异常
     * <p>
     * <p>
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p> An attempt is made to write up to <i>r</i> bytes to the channel,
     * where <i>r</i> is the number of bytes remaining in the buffer, that is,
     * {@code src.remaining()}, at the moment this method is invoked.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is written, where
     * {@code 0}&nbsp;{@code <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;<i>r</i>.
     * This byte sequence will be transferred from the buffer starting at index
     * <i>p</i>, where <i>p</i> is the buffer's position at the moment this
     * method is invoked; the index of the last byte written will be
     * <i>p</i>&nbsp;{@code +}&nbsp;<i>n</i>&nbsp;{@code -}&nbsp;{@code 1}.
     * Upon return the buffer's position will be equal to
     * <i>p</i>&nbsp;{@code +}&nbsp;<i>n</i>; its limit will not have changed.
     *
     * <p> Unless otherwise specified, a write operation will return only after
     * writing all of the <i>r</i> requested bytes.  Some types of channels,
     * depending upon their state, may write only some of the bytes or possibly
     * none at all.  A socket channel in non-blocking mode, for example, cannot
     * write any more bytes than are free in the socket's output buffer.
     *
     * <p> This method may be invoked at any time.  If another thread has
     * already initiated a write operation upon this channel, however, then an
     * invocation of this method will block until the first operation is
     * complete. </p>
     *
     * @param src The buffer from which bytes are to be retrieved
     * @return The number of bytes written, possibly zero
     * @throws NonWritableChannelException If this channel was not opened for writing
     * @throws ClosedChannelException      If this channel is closed
     * @throws AsynchronousCloseException  If another thread closes this channel
     *                                     while the write operation is in progress
     * @throws ClosedByInterruptException  If another thread interrupts the current thread
     *                                     while the write operation is in progress, thereby
     *                                     closing the channel and setting the current thread's
     *                                     interrupt status
     * @throws IOException                 If some other I/O error occurs
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        return channel.write(src);
    }

    /**
     * 这段代码是Java中一个通道(Channel)类的文档注释，描述了isOpen()方法的功能：
     * 功能：判断当前通道是否处于打开状态
     * 返回值：当且仅当通道打开时返回true，否则返回false
     * 作用：用于检查通道的可用性状态，通常在网络编程或文件I/O操作中使用
     * Tells whether or not this channel is open.
     *
     * @return {@code true} if, and only if, this channel is open
     */
    @Override
    public boolean isOpen() {
        return isOpen;
    }

    /**
     *
     * 强制依赖close 方法执行，才能上传文件
     *
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
        channel.close();
        if (!isOpen) {
            return;
        }
        // 上传文件到对象存储
        s3Util.uploadLocalFile(s3SftpPath, tempFile);
        // 删除临时文件
        Files.deleteIfExists(tempFile);

        this.isOpen = false;
    }
}
