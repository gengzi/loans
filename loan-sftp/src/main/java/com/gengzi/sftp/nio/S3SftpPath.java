package com.gengzi.sftp.nio;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Iterator;
import java.util.LinkedList;

import static com.gengzi.sftp.nio.S3SftpFileSystemProvider.checkPath;
import static com.gengzi.sftp.nio.constans.Constans.PATH_SEPARATOR;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

public class S3SftpPath implements Path {


    private final S3SftpFileSystem fileSystem;
    private final S3SftpPosixLikePathRepresentation pathRepresentation;

    public S3SftpPath(S3SftpFileSystem fileSystem, S3SftpPosixLikePathRepresentation pathRepresentation) {
        this.fileSystem = fileSystem;
        this.pathRepresentation = pathRepresentation;
    }


    /**
     * 获取一个时sftp文件操作类
     * @param s3SftpFileSystem
     * @param first first 为路径的初始部分
     * @Param more 为路径的后续部分（自动用文件系统的分隔符拼接）
     * @return
     */
    public static Path getPath(S3SftpFileSystem s3SftpFileSystem, String first,String... more) {
        return new S3SftpPath(s3SftpFileSystem, S3SftpPosixLikePathRepresentation.of(first,more));
    }

    @NotNull
    @Override
    public S3SftpFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return pathRepresentation.isAbsolute();
    }

    @Override
    public Path getRoot() {
        return null;
    }

    @Override
    public Path getFileName() {
        return null;
    }

    @Override
    public Path getParent() {
        return null;
    }

    @Override
    public int getNameCount() {
        return 0;
    }

    @NotNull
    @Override
    public Path getName(int index) {
        return null;
    }

    @NotNull
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return null;
    }

    @Override
    public boolean startsWith(@NotNull Path other) {
        return false;
    }

    @Override
    public boolean startsWith(@NotNull String other) {
        return Path.super.startsWith(other);
    }

    @Override
    public boolean endsWith(@NotNull Path other) {
        return false;
    }

    @Override
    public boolean endsWith(@NotNull String other) {
        return Path.super.endsWith(other);
    }

    /**
     * 规范化路径：移除 .（当前目录）和 ..（父目录）等冗余组件
     * @return
     */
    @NotNull
    @Override
    public Path normalize() {
        // 如果是根目录，返回this
        if(pathRepresentation.isRoot()){
            return this;
        }
        // 判断是否目录
        boolean directory = pathRepresentation.isDirectory();

        final var elements = pathRepresentation.elements();
        final var realElements = new LinkedList<String>();

        if (this.isAbsolute()) {
            realElements.add(PATH_SEPARATOR);
        }

        for (var element : elements) {
            if (element.equals(".")) {
                continue;
            }
            if (element.equals("..")) {
                if (!realElements.isEmpty()) {
                    realElements.removeLast();
                }
                continue;
            }

            if (directory) {
                realElements.addLast(element + "/");
            } else {
                realElements.addLast(element);
            }
        }
        return S3SftpPath.getPath(fileSystem, String.join(PATH_SEPARATOR, realElements));


    }

    @NotNull
    @Override
    public Path resolve(@NotNull Path other) {
        S3SftpPath s3Other = checkPath(other);

        if (!this.bucketName().equals(s3Other.bucketName())) {
            throw new IllegalArgumentException("S3Paths cannot be resolved when they are from different buckets");
        }

        if (s3Other.isAbsolute()) {
            return s3Other;
        }
        if (s3Other.isEmpty()) {
            return this;
        }

        String concatenatedPath;
        if (!this.pathRepresentation.hasTrailingSeparator()) {
            concatenatedPath = this + PATH_SEPARATOR + s3Other;
        } else {
            concatenatedPath = this.toString() + s3Other;
        }

        return from(concatenatedPath);
    }
    /**
     * Construct a path using the same filesystem (bucket) as this path
     */
    private S3SftpPath from(String path) {
        return (S3SftpPath) getPath(this.fileSystem, path);
    }

    @NotNull
    @Override
    public Path resolve(@NotNull String other) {
        return resolve(from(other));
    }

    @NotNull
    @Override
    public Path resolveSibling(@NotNull Path other) {
        return Path.super.resolveSibling(other);
    }

    @NotNull
    @Override
    public Path resolveSibling(@NotNull String other) {
        return Path.super.resolveSibling(other);
    }

    @NotNull
    @Override
    public Path relativize(@NotNull Path other) {
        return null;
    }

    @NotNull
    @Override
    public URI toUri() {
        return null;
    }

    @NotNull
    @Override
    public S3SftpPath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        }
        return new S3SftpPath(fileSystem, S3SftpPosixLikePathRepresentation.of(PATH_SEPARATOR, pathRepresentation.toString()));
    }

    @NotNull
    @Override
    public Path toRealPath(@NotNull LinkOption... options)  {
        S3SftpPath path = this;
        if (!isAbsolute()) {
            return toAbsolutePath();
        }
        return S3SftpPath.getPath(fileSystem, PATH_SEPARATOR, path.normalize().toString());
    }

    @NotNull
    @Override
    public File toFile() {
        throw new UnsupportedOperationException("S3 Objects cannot be represented in the local (default) file system");
    }

    @NotNull
    @Override
    public WatchKey register(@NotNull WatchService watcher, WatchEvent.Kind<?>[] events, @NotNull WatchEvent.Modifier... modifiers) throws IOException {
        return null;
    }

    @NotNull
    @Override
    public WatchKey register(@NotNull WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return Path.super.register(watcher, events);
    }

    @NotNull
    @Override
    public Iterator<Path> iterator() {
        return Path.super.iterator();
    }

    @Override
    public int compareTo(@NotNull Path other) {
        return 0;
    }

    public String bucketName() {
        return fileSystem.bucketName();
    }
    private boolean isEmpty() {
        return pathRepresentation.toString().isEmpty();
    }

    public String getKey() {
        if (isEmpty()) {
            return "";
        }
        var s = toRealPath(NOFOLLOW_LINKS).toString();
        if (s.startsWith(PATH_SEPARATOR + bucketName())) {
            s = s.replaceFirst(PATH_SEPARATOR + bucketName(), "");
        }
        while (s.startsWith(PATH_SEPARATOR)) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * 判断是否为目录
     *
     * 以 / 结尾的认为是目录
     *
     * @return
     */
    public boolean isDirectory() {
        return pathRepresentation.isDirectory();
    }

    @Override
    public String toString() {
        return pathRepresentation.toString();
    }



}
