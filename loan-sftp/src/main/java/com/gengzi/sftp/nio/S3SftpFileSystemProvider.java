package com.gengzi.sftp.nio;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.nio.spi.s3.S3BasicFileAttributes;
import software.amazon.nio.spi.s3.S3Path;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于创建返回 FileSystem 的工厂类
 * 负责创建和管理 FileSystem 实例
 */
public class S3SftpFileSystemProvider extends FileSystemProvider {

    // 约束
    static final String SCHEME = "s3sftp";
    private static final Map<String, S3SftpFileSystem> FS_CACHE = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public String getScheme() {
        return SCHEME;
    }

    /**
     * 解析uri 创建filesystem
     *
     * @param uri URI reference  s3sftp://[key:secret@]endpoint[:port]/bucket/objectkey
     * @param env A map of provider specific properties to configure the file system;
     *            may be empty   环境变量
     * @return
     * @throws IOException
     */
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        // 判断uri 的约束是否一致
        if (!uri.getScheme().equals(getScheme())) {
            throw new IllegalArgumentException("URI scheme must be " + getScheme());
        }
        // 解析uri 和 env
        return getFileSystem(uri);
    }


    @Override
    public FileSystem getFileSystem(URI uri) {
        S3SftpFileSystemInfo info = new S3SftpFileSystemInfo(uri);
        return FS_CACHE.computeIfAbsent(info.key(), (key) -> {
            S3SftpNioSpiConfiguration config = new S3SftpNioSpiConfiguration().withEndpoint(info.endpoint()).withBucketName(info.bucket());
            if (info.accessKey() != null) {
                config.withCredentials(info.accessKey(), info.accessSecret());
            }
            return new S3SftpFileSystem(this, config);
        });
    }

    @NotNull
    @Override
    public Path getPath(@NotNull URI uri) {
        return getFileSystem(uri).getPath(uri.getScheme() + ":/" + uri.getPath());
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null;
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

    }

    @Override
    public void delete(Path path) throws IOException {

    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {

    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    /**
     * 获取 path 的属性信息
     * @param path
     *          the path to the file
     * @param type
     *          the {@code Class} of the file attributes required
     *          to read
     * @param options
     *          options indicating how symbolic links are handled
     *
     * @return
     * @param <A>
     * @throws IOException
     */
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {

        Objects.requireNonNull(type);
        Path s3Path = checkPath(path);

        if (type.equals(BasicFileAttributes.class)) {
            @SuppressWarnings("unchecked")
            A a = (A) S3SftpBasicFileAttributes.get((S3SftpPath) s3Path,null);
            return a;
        } else {
            throw new UnsupportedOperationException("cannot read attributes of type: " + type);
        }

    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {

        Objects.requireNonNull(attributes);
        var s3Path = checkPath(path);

        if (s3Path.isDirectory() || attributes.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        return S3SftpBasicFileAttributes.get(s3Path, Duration.ofMinutes(5)).toMap();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {

    }

    static S3SftpPath checkPath(Path obj) {
        Objects.requireNonNull(obj);
        if (!(obj instanceof S3SftpPath)) {
            throw new ProviderMismatchException();
        }
        return (S3SftpPath) obj;
    }


}
