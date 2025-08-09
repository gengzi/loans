package com.gengzi.sftp.nio;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Iterator;

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
        return false;
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

    @NotNull
    @Override
    public Path normalize() {
        return null;
    }

    @NotNull
    @Override
    public Path resolve(@NotNull Path other) {
        return null;
    }

    @NotNull
    @Override
    public Path resolve(@NotNull String other) {
        return Path.super.resolve(other);
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
    public Path toAbsolutePath() {
        return null;
    }

    @NotNull
    @Override
    public Path toRealPath(@NotNull LinkOption... options)  {
        return null;
    }

    @NotNull
    @Override
    public File toFile() {
        return Path.super.toFile();
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

    String bucketName() {
        return fileSystem.bucketName();
    }
    private boolean isEmpty() {
        return pathRepresentation.toString().isEmpty();
    }

    String getKey() {
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

    public boolean isDirectory() {
        return false;
    }
}
