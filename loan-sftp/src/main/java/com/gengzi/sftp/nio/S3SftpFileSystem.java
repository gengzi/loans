package com.gengzi.sftp.nio;

import com.gengzi.sftp.nio.constans.Constans;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;


/**
 * 提供了访问文件系统资源（如文件、目录）的入口点
 */
public class S3SftpFileSystem extends FileSystem {

    // s3 只能支持basic 视图，不支持 unix 的 posix 视图，不支持 win 的dos 视图
    static final String BASIC_FILE_ATTRIBUTE_VIEW = "basic";
    private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS =
            Collections.singleton(BASIC_FILE_ATTRIBUTE_VIEW);
    private final S3SftpFileSystemProvider s3SftpFileSystemProvider;
    private final S3SftpNioSpiConfiguration s3SftpNioSpiConfiguration;
    private final String bucketName;
    private final S3SftpClientProvider s3SftpClientProvider;
    private boolean open = true;


    /**
     * 创建文件系统
     *
     * @param provider
     * @Param config
     */
    public S3SftpFileSystem(S3SftpFileSystemProvider provider, S3SftpNioSpiConfiguration config) {
        this.s3SftpFileSystemProvider = provider;
        this.s3SftpNioSpiConfiguration = config;
        // 桶信息
        this.bucketName = config.getBucketName();
        // 根据配置创建s3client
        this.s3SftpClientProvider = new S3SftpClientProvider(config);
    }

    public S3AsyncClient client() {
        return s3SftpClientProvider.generateClient(bucketName);
    }


    @Override
    public FileSystemProvider provider() {
        return this.s3SftpFileSystemProvider;
    }

    @Override
    public void close() throws IOException {
    //TODO 等代补充

    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return Constans.PATH_SEPARATOR;
    }

    /**
     * 返回文件系统的根目录
     *
     * @return
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton(S3SftpPath.getPath(this, "/"));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.EMPTY_SET;
    }

    /**
     * 返回文件系统支持的文件属性视图
     * @return
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
    }

    /**
     * 将字符串路径转换为 Path 对象
     *
     * @param first the path string or initial part of the path string
     * @param more  additional strings to be joined to form the path string
     * @return
     */
    @NotNull
    @Override
    public Path getPath(@NotNull String first, @NotNull String... more) {
        return S3SftpPath.getPath(this, first, more);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return FileSystems.getDefault().getPathMatcher(syntaxAndPattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException(
                "This method is not yet supported. Please raise a feature request describing your use case");
    }

    /**
     * 文件系统事件监控服务
     * 用于监听文件或目录的变化（如创建、删除、修改）
     * @return
     * @throws IOException
     */
    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("Watch service N/A");
    }

    String bucketName() {
        return bucketName;
    }
}
