package com.gengzi.sftp.nio;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
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
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * 用于创建返回 FileSystem 的工厂类
 * 负责创建和管理 FileSystem 实例
 */
public class S3SftpFileSystemProvider extends FileSystemProvider {
    // 文件系统前缀标识
    static final String SCHEME = "s3sftp";
    static final String PATH_SEPARATOR = "/";
    private static final Map<String, S3SftpFileSystem> FS_CACHE = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    static S3SftpPath checkPath(Path obj) {
        Objects.requireNonNull(obj);
        if (!(obj instanceof S3SftpPath)) {
            throw new ProviderMismatchException();
        }
        return (S3SftpPath) obj;
    }

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
        Objects.requireNonNull(uri);
        return getFileSystem(uri)
                .getPath(uri.getScheme() + ":/" + uri.getPath());
    }

    /**
     * 用于为指定路径创建一个 可随机访问的字节通道（SeekableByteChannel），支持读写文件数据并灵活控制文件打开模式与初始属性
     *
     * @param path    the path to the file to open or create
     * @param options options specifying how the file is opened
     * @param attrs   an optional list of file attributes to set atomically when
     *                creating the file
     * @return
     * @throws IOException
     */
    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        checkPath(path);
        S3SftpFileSystem fileSystem = (S3SftpFileSystem) path.getFileSystem();
        S3SftpSeekableByteChannel s3SftpSeekableByteChannel = new S3SftpSeekableByteChannel((S3SftpPath) path,
                fileSystem.client(), options);
        fileSystem.registerOpenChannel(s3SftpSeekableByteChannel);
        return s3SftpSeekableByteChannel;
    }

    /**
     * 创建一个目录流，遍历指定目录（dir）中符合过滤条件（filter）的文件和子目录
     *
     * @param dir    the path to the directory
     * @param filter the directory stream filter
     * @return
     * @throws IOException
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        // 检查路径是否为s3sftp路径
        S3SftpPath s3Path = checkPath(dir);
        // 获取一个绝对路径
        String path = s3Path.toAbsolutePath().getKey();
        if (!s3Path.isDirectory()) {
            path = path + PATH_SEPARATOR;
        }
        return new S3SftpDirectoryStream(s3Path.getFileSystem(), s3Path.bucketName(), path, filter);
    }

    /**
     * 用于在指定路径创建新目录并设置初始属性
     *
     * @param dir   the directory to create
     * @param attrs an optional list of file attributes to set atomically when
     *              creating the directory
     * @throws IOException
     */
    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        S3SftpPath s3Directory = checkPath(dir);
        // 判断路径是否存在
        if (s3Directory.toString().equals(PATH_SEPARATOR) || s3Directory.toString().isEmpty()) {
            throw new FileAlreadyExistsException("Root directory already exists");
        }
        String directoryKey = s3Directory.toRealPath(LinkOption.NOFOLLOW_LINKS).getKey();
        if (!directoryKey.endsWith(PATH_SEPARATOR) && !directoryKey.isEmpty()) {
            directoryKey = directoryKey + PATH_SEPARATOR;
        }
        try {
            s3Directory.getFileSystem().client().putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Directory.bucketName())
                            .key(directoryKey)
                            .build(),
                    AsyncRequestBody.empty()
            ).get(1L, MINUTES);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 用于删除指定路径对应的文件或目录，是文件系统操作中删除资源的核心方法
     *
     * @param path the path to the file to delete
     * @throws IOException
     */
    @Override
    public void delete(Path path) throws IOException {
        S3SftpPath deletePath = checkPath(path);
        String deletePathKey = deletePath.toRealPath(LinkOption.NOFOLLOW_LINKS).getKey();
        // 判断如果是根目录，不允许删除
        S3AsyncClient s3Client = deletePath.getFileSystem().client();
        S3SftpBasicFileAttributes s3SftpBasicFileAttributes = S3SftpBasicFileAttributes.get(deletePath, null);
        boolean directory = s3SftpBasicFileAttributes.isDirectory();
        String bucketName = deletePath.bucketName();
        if(!directory){
            // 是文件，可以删除
            delPath(s3Client, bucketName, deletePathKey);
        }
        if(directory){
            boolean emptyDirectory = s3SftpBasicFileAttributes.isEmptyDirectory();
            if(emptyDirectory){
                // 是空目录，可以删除
                delPath(s3Client, bucketName, deletePathKey);
            }else{
                throw new DirectoryNotEmptyException("dir is not empty");
            }
        }
    }

    private static void delPath(S3AsyncClient s3Client, String bucketName, String deletePathKey) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(deletePathKey)
                            .build()
            ).get(1L, MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * 检查当前程序对指定路径（文件 / 目录）是否拥有特定的访问权限，若权限不足则直接抛出异常，无异常则表示权限满足。
     *
     * @param path  the path to the file to check
     * @param modes The access modes to check; may have zero elements
     * @throws IOException
     */
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        S3SftpPath checkPath = checkPath(path);
        S3SftpPath realPath = checkPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
        try {

            S3SftpBasicFileAttributes.get((S3SftpPath) realPath, null);

        } catch (NoSuchFileException e) {
            throw new NoSuchFileException(realPath.toString());
        } catch (IOException e) {
            throw new IOException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    /**
     * 获取 path 的属性信息
     *
     * @param path    the path to the file
     * @param type    the {@code Class} of the file attributes required
     *                to read
     * @param options options indicating how symbolic links are handled
     * @param <A>
     * @return
     * @throws IOException
     */
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {

        Objects.requireNonNull(type);
        Path s3Path = checkPath(path);

        if (type.equals(BasicFileAttributes.class)) {
            @SuppressWarnings("unchecked")
            A a = (A) S3SftpBasicFileAttributes.get((S3SftpPath) s3Path, null);
            return a;
        } else {
            throw new UnsupportedOperationException("cannot read attributes of type: " + type);
        }

    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {

        Objects.requireNonNull(attributes);
        var s3Path = checkPath(path);

        if (attributes.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        return S3SftpBasicFileAttributes.get(s3Path, Duration.ofMinutes(5)).toMap();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {

    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        S3SftpFileSystem fs = (S3SftpFileSystem) getFileSystem(path.toUri());
        S3SftpSeekableByteChannel s3SeekableByteChannel = new S3SftpSeekableByteChannel((S3SftpPath) path, fs.client(), options);
        return new S3SftpFileChannel(s3SeekableByteChannel);
    }

    /**
     * 判断文件是否存在
     *
     * @param s3SftpPath 文件
     */
    public Boolean exists(S3AsyncClient s3AsyncClient, S3SftpPath s3SftpPath) {
        try {
            //TODO 配置类
            s3AsyncClient.headObject(HeadObjectRequest.builder().bucket(s3SftpPath.bucketName()).key(s3SftpPath.getKey()).build())
                    .get(1L, MINUTES);
            return true;
        } catch (ExecutionException | NoSuchKeyException | InterruptedException | TimeoutException e) {
            logger.debug("Could not retrieve object head information", e);
            return false;
        }


    }
}
