package com.gengzi.sftp.nio;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.amazon.nio.spi.s3.util.TimeOutUtils.createAndLogTimeOutMessage;

public class S3SftpBasicFileAttributes implements BasicFileAttributes {

    private static final Logger logger = LoggerFactory.getLogger(S3SftpBasicFileAttributes.class.getName());


    private final FileTime lastModifiedTime;
    private final Long size;
    private final Object eTag;
    private final boolean isDirectory;
    private final boolean isRegularFile;

    public S3SftpBasicFileAttributes(FileTime lastModifiedTime,
                                     Long size,
                                     Object eTag,
                                     boolean isDirectory,
                                     boolean isRegularFile) {
        this.lastModifiedTime = lastModifiedTime;
        this.size = size;
        this.eTag = eTag;
        this.isDirectory = isDirectory;
        this.isRegularFile = isRegularFile;

    }

    public static S3SftpBasicFileAttributes get(S3SftpPath path, Duration duration) throws IOException {
        // 如果是目录就返回固定的属性
        if (path.isDirectory()) {
            // return DIRECTORY_ATTRIBUTES;
        }
        // 是文件，调用s3返回文件属性
        var headResponse = getObjectMetadata(path, Duration.ofMinutes(5));
        return new S3SftpBasicFileAttributes(
                FileTime.from(headResponse.lastModified()),
                headResponse.contentLength(),
                headResponse.eTag(),
                false,
                true
        );

    }

    private static HeadObjectResponse getObjectMetadata(
            S3SftpPath path,
            Duration timeout
    ) throws IOException {
        var client = path.getFileSystem().client();
        var bucketName = path.bucketName();
        try {
            return client.headObject(req -> req
                    .bucket(bucketName)
                    .key(path.getKey())
            ).get(timeout.toMillis(), MILLISECONDS);
        } catch (ExecutionException e) {
            var errMsg = format(
                    "an '%s' error occurred while obtaining the metadata (for operation getFileAttributes) of '%s'" +
                            "that was not handled successfully by the S3Client's configured RetryConditions",
                    e.getCause().toString(), path.toUri());
            logger.error(errMsg);
            throw new IOException(errMsg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (TimeoutException e) {
            var msg = createAndLogTimeOutMessage(logger, "getFileAttributes", timeout.toMillis(), MILLISECONDS);
            throw new IOException(msg, e);
        }
    }

    /**
     * Returns the time of last modification.
     *
     * <p> If the file system implementation does not support a time stamp
     * to indicate the time of last modification then this method returns an
     * implementation specific default value, typically a {@code FileTime}
     * representing the epoch (1970-01-01T00:00:00Z).
     *
     * @return a {@code FileTime} representing the time the file was last
     * modified
     */
    @Override
    public FileTime lastModifiedTime() {
        return null;
    }

    /**
     * Returns the time of last access.
     *
     * <p> If the file system implementation does not support a time stamp
     * to indicate the time of last access then this method returns
     * an implementation specific default value, typically the {@link
     * #lastModifiedTime() last-modified-time} or a {@code FileTime}
     * representing the epoch (1970-01-01T00:00:00Z).
     *
     * @return a {@code FileTime} representing the time of last access
     */
    @Override
    public FileTime lastAccessTime() {
        return null;
    }

    /**
     * Returns the creation time. The creation time is the time that the file
     * was created.
     *
     * <p> If the file system implementation does not support a time stamp
     * to indicate the time when the file was created then this method returns
     * an implementation specific default value, typically the {@link
     * #lastModifiedTime() last-modified-time} or a {@code FileTime}
     * representing the epoch (1970-01-01T00:00:00Z).
     *
     * @return a {@code FileTime} representing the time the file was created
     */
    @Override
    public FileTime creationTime() {
        return null;
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     *
     * @return {@code true} if the file is a regular file with opaque content
     */
    @Override
    public boolean isRegularFile() {
        return false;
    }

    /**
     * Tells whether the file is a directory.
     *
     * @return {@code true} if the file is a directory
     */
    @Override
    public boolean isDirectory() {
        return false;
    }

    /**
     * Tells whether the file is a symbolic link.
     *
     * @return {@code true} if the file is a symbolic link
     */
    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    /**
     * Tells whether the file is something other than a regular file, directory,
     * or symbolic link.
     *
     * @return {@code true} if the file something other than a regular file,
     * directory or symbolic link
     */
    @Override
    public boolean isOther() {
        return false;
    }

    /**
     * Returns the size of the file (in bytes). The size may differ from the
     * actual size on the file system due to compression, support for sparse
     * files, or other reasons. The size of files that are not {@link
     * #isRegularFile regular} files is implementation specific and
     * therefore unspecified.
     *
     * @return the file size, in bytes
     */
    @Override
    public long size() {
        return 0;
    }

    /**
     * Returns an object that uniquely identifies the given file, or {@code
     * null} if a file key is not available. On some platforms or file systems
     * it is possible to use an identifier, or a combination of identifiers to
     * uniquely identify a file. Such identifiers are important for operations
     * such as file tree traversal in file systems that support <a
     * href="../package-summary.html#links">symbolic links</a> or file systems
     * that allow a file to be an entry in more than one directory. On UNIX file
     * systems, for example, the <em>device ID</em> and <em>inode</em> are
     * commonly used for such purposes.
     *
     * <p> The file key returned by this method can only be guaranteed to be
     * unique if the file system and files remain static. Whether a file system
     * re-uses identifiers after a file is deleted is implementation dependent and
     * therefore unspecified.
     *
     * <p> File keys returned by this method can be compared for equality and are
     * suitable for use in collections. If the file system and files remain static,
     * and two files are the {@link Files#isSameFile same} with
     * non-{@code null} file keys, then their file keys are equal.
     *
     * @return an object that uniquely identifies the given file, or {@code null}
     * @see Files#walkFileTree
     */
    @Override
    public Object fileKey() {
        return null;
    }

    public Map<String, Object> toMap() {

        return Map.of(
                "lastModifiedTime", lastModifiedTime(),
                "lastAccessTime", lastAccessTime(),
                "creationTime", creationTime(),
                "isRegularFile", isRegularFile(),
                "isDirectory", isDirectory(),
                "isSymbolicLink", isSymbolicLink(),
                "isOther", isOther(),
                "size", size(),
                "fileKey", fileKey()
        );
    }
}
