package com.gengzi.sftp.nio;


import com.gengzi.sftp.nio.constans.Constans;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.amazon.nio.spi.s3.util.TimeOutUtils.createAndLogTimeOutMessage;

public class S3SftpBasicFileAttributes implements BasicFileAttributes {

    private static final Logger logger = LoggerFactory.getLogger(S3SftpBasicFileAttributes.class.getName());
    // 修改声明：为目录和文件都添加了权限，支持用户用户组，和其他用户组的可读可写
    private static final Set<PosixFilePermission> posixFilePermissions;
    static {
        posixFilePermissions = new HashSet<>();
        posixFilePermissions.add(PosixFilePermission.OWNER_READ);
        posixFilePermissions.add(PosixFilePermission.OWNER_WRITE);
        posixFilePermissions.add(PosixFilePermission.OTHERS_READ);
        posixFilePermissions.add(PosixFilePermission.OTHERS_WRITE);
        posixFilePermissions.add(PosixFilePermission.GROUP_READ);
        posixFilePermissions.add(PosixFilePermission.GROUP_WRITE);
    }
    private static final S3SftpBasicFileAttributes DIRECTORY_ATTRIBUTES = new S3SftpBasicFileAttributes(
            FileTime.fromMillis(0),
            0L,
            null,
            true,
            false,
            posixFilePermissions
    );



    private final FileTime lastModifiedTime;
    private final Long size;
    private final Object eTag;
    private final boolean isDirectory;
    private final boolean isRegularFile;
    private final Set<PosixFilePermission> permissions;


    public S3SftpBasicFileAttributes(FileTime lastModifiedTime,
                                     Long size,
                                     Object eTag,
                                     boolean isDirectory,
                                     boolean isRegularFile,
                                     Set<PosixFilePermission> permissions) {
        this.lastModifiedTime = lastModifiedTime;
        this.size = size;
        this.eTag = eTag;
        this.isDirectory = isDirectory;
        this.isRegularFile = isRegularFile;
        this.permissions = permissions;

    }

    public static S3SftpBasicFileAttributes get(S3SftpPath path, Duration duration) throws IOException {

        // 如果是目录就返回固定的属性
        if (path.isDirectory()) {
            return DIRECTORY_ATTRIBUTES;
        }
        // 是文件，调用s3返回文件属性
        var headResponse = getObjectMetadata(path, Duration.ofMinutes(5));
        if(headResponse == null){
            // 不存在该键，判断是否为目录
            String dir = normalizePath(path.getKey());
            ListObjectsV2Publisher result = listObjectsV2Paginator(path, dir);
            SdkPublisher<S3Object> contents = result.contents();
            SdkPublisher<CommonPrefix> commonPrefixSdkPublisher = result.commonPrefixes();
            Single<Boolean> empty = Flowable.concat(commonPrefixSdkPublisher, contents).isEmpty();
            if(!empty.blockingGet()){
                return DIRECTORY_ATTRIBUTES;
            }else{
               return null;
            }


        }
        return new S3SftpBasicFileAttributes(
                FileTime.from(headResponse.lastModified()),
                headResponse.contentLength(),
                headResponse.eTag(),
                false,
                true,
                posixFilePermissions
        );

    }

    /**
     * 规范化路径：确保路径以 "/" 结尾，统一前缀格式
     * 例如："docs" → "docs/", "docs/" → "docs/"
     */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return ""; // 空路径表示根目录
        }
        return path.endsWith("/") ? path : path + "/";
    }


    private static ListObjectsV2Publisher listObjectsV2Paginator(S3SftpPath path, String dir){
        S3AsyncClient client = path.getFileSystem().client();
        ListObjectsV2Publisher listObjectsV2Publisher = client.listObjectsV2Paginator(req -> req
                .bucket(path.bucketName())
                .prefix(dir)
                .delimiter(Constans.PATH_SEPARATOR));
        return listObjectsV2Publisher;
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

        } catch (NoSuchKeyException e){
            return null;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if(cause instanceof NoSuchKeyException){
                return null;
            }
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
        return lastModifiedTime;
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
        return lastModifiedTime();
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
        return lastModifiedTime();
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     *
     * @return {@code true} if the file is a regular file with opaque content
     */
    @Override
    public boolean isRegularFile() {
        return isRegularFile;
    }

    /**
     * Tells whether the file is a directory.
     *
     * @return {@code true} if the file is a directory
     */
    @Override
    public boolean isDirectory() {
        return isDirectory;
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
        return size;
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
        return eTag;
    }

    public Map<String, Object> toMap() {
        return new HashMap<String, Object>() {{
            put("lastModifiedTime", lastModifiedTime());
            put("lastAccessTime", lastAccessTime());
            put("creationTime", creationTime());
            put("isRegularFile", isRegularFile());
            put("isDirectory", isDirectory());
            put("isSymbolicLink", isSymbolicLink());
            put("isOther", isOther());
            put("size", size());
            put("fileKey", fileKey());
            put("permissions", permissions());
        }};
    }

    private Set<PosixFilePermission> permissions() {
        return permissions;
    }
}
