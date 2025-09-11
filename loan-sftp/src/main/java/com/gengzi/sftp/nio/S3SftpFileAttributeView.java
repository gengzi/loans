package com.gengzi.sftp.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Duration;

public class S3SftpFileAttributeView implements BasicFileAttributeView {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final S3SftpPath path;

    public S3SftpFileAttributeView(S3SftpPath path) {
        this.path = path;
    }

    /**
     * Returns the name of the attribute view. Attribute views of this type
     * have the name {@code "basic"}.
     */
    @Override
    public String name() {
        return "s3Sftp";
    }

    /**
     *
     * 这段代码是Java中一个方法的文档注释，用于批量读取文件的基本属性。
     * 主要功能：
     * 一次性获取文件的多个基本属性信息
     * 具体实现可能以原子操作方式执行，确保属性读取的一致性
     * Reads the basic file attributes as a bulk operation.
     *
     * <p> It is implementation specific if all file attributes are read as an
     * atomic operation with respect to other file system operations.
     *
     * @return the file attributes
     * @throws IOException       if an I/O error occurs
     * @throws SecurityException In the case of the default provider, a security manager is
     *                           installed, its {@link SecurityManager#checkRead(String) checkRead}
     *                           method is invoked to check read access to the file
     */
    @Override
    public BasicFileAttributes readAttributes() throws IOException {
       return S3SftpBasicFileAttributes.get(path, Duration.ofMinutes(1L));
    }

    /**
     * Updates any or all of the file's last modified time, last access time,
     * and create time attributes.
     *
     * <p> This method updates the file's timestamp attributes. The values are
     * converted to the epoch and precision supported by the file system.
     * Converting from finer to coarser granularities result in precision loss.
     * The behavior of this method when attempting to set a timestamp that is
     * not supported or to a value that is outside the range supported by the
     * underlying file store is not defined. It may or not fail by throwing an
     * {@code IOException}.
     *
     * <p> If any of the {@code lastModifiedTime}, {@code lastAccessTime},
     * or {@code createTime} parameters has the value {@code null} then the
     * corresponding timestamp is not changed. An implementation may require to
     * read the existing values of the file attributes when only some, but not
     * all, of the timestamp attributes are updated. Consequently, this method
     * may not be an atomic operation with respect to other file system
     * operations. Reading and re-writing existing values may also result in
     * precision loss. If all of the {@code lastModifiedTime}, {@code
     * lastAccessTime} and {@code createTime} parameters are {@code null} then
     * this method has no effect.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to change a file's last access time.
     * <pre>
     *    Path path = ...
     *    FileTime time = ...
     *    Files.getFileAttributeView(path, BasicFileAttributeView.class).setTimes(null, time, null);
     * </pre>
     *
     * @param lastModifiedTime the new last modified time, or {@code null} to not change the
     *                         value
     * @param lastAccessTime   the last access time, or {@code null} to not change the value
     * @param createTime       the file's create time, or {@code null} to not change the value
     * @throws IOException       if an I/O error occurs
     * @throws SecurityException In the case of the default provider, a security manager is
     *                           installed, its  {@link SecurityManager#checkWrite(String) checkWrite}
     *                           method is invoked to check write access to the file
     * @see Files#setLastModifiedTime
     */
    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        logger.warn("S3 doesn't support setting of file times other than by writing the file. " +
                "The time set during those operations will be determined by S3. This method call will be ignored");

    }
}
