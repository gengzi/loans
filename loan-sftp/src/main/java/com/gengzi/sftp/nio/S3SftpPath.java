package com.gengzi.sftp.nio;

import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import static com.gengzi.sftp.nio.S3SftpFileSystemProvider.checkPath;
import static com.gengzi.sftp.nio.constans.Constants.PATH_SEPARATOR;
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
     *
     * @param s3SftpFileSystem
     * @param first            first 为路径的初始部分
     * @return
     * @Param more 为路径的后续部分（自动用文件系统的分隔符拼接）
     */
    public static S3SftpPath getPath(S3SftpFileSystem s3SftpFileSystem, String first, String... more) {
        return new S3SftpPath(s3SftpFileSystem, S3SftpPosixLikePathRepresentation.of(first, more));
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
    public S3SftpPath getRoot() {
        return getPath(this.fileSystem, PATH_SEPARATOR);
    }

    @Override
    public Path getFileName() {
        final var elements = pathRepresentation.elements();
        var size = elements.size();
        if (size == 0) {
            return null;
        }

        if (pathRepresentation.hasTrailingSeparator()) {
            return from(elements.get(size - 1) + PATH_SEPARATOR);
        } else {
            return from(elements.get(size - 1));
        }
    }

    @Override
    public Path getParent() {
        var size = pathRepresentation.elements().size();
        if (this.equals(getRoot()) || size < 1) {
            return null;
        }
        if (pathRepresentation.isAbsolute() && size == 1) {
            return getRoot();
        }
        return subpath(0, getNameCount() - 1);
    }

    @Override
    public int getNameCount() {
        return pathRepresentation.elements().size();
    }

    @NotNull
    @Override
    public Path getName(int index) {
        final var elements = pathRepresentation.elements();
        if (index < 0 || index >= elements.size()) {
            throw new IllegalArgumentException("index must be >= 0 and <= the number of path elements");
        }
        return subpath(index, index + 1);
    }

    @NotNull
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        final var size = pathRepresentation.elements().size();
        if (beginIndex < 0) {
            throw new IllegalArgumentException("begin index may not be < 0");
        }
        if (beginIndex >= size) {
            throw new IllegalArgumentException("begin index may not be >= the number of path elements");
        }
        if (endIndex > size) {
            throw new IllegalArgumentException("end index may not be > the number of path elements");
        }
        if (endIndex <= beginIndex) {
            throw new IllegalArgumentException("end index may not be <= the begin index");
        }

        var path = String.join(PATH_SEPARATOR, pathRepresentation.elements().subList(beginIndex, endIndex));
        if (endIndex == size && !pathRepresentation.hasTrailingSeparator()) {
            return from(path);
        } else {
            return from(path + PATH_SEPARATOR);
        }
    }

    @Override
    public boolean startsWith(@NotNull Path other) {
        return this.equals(other) ||
                this.fileSystem.equals(other.getFileSystem()) &&
                        this.isAbsolute() == other.isAbsolute() &&
                        this.getNameCount() >= other.getNameCount() &&
                        this.subpath(0, other.getNameCount()).equals(other);
    }

    @Override
    public boolean startsWith(@NotNull String other) {
        return startsWith(getPath(fileSystem, other));
    }

    @Override
    public boolean endsWith(@NotNull Path other) {
        return this.equals(other) ||
                this.fileSystem == other.getFileSystem() &&
                        this.getNameCount() >= other.getNameCount() &&
                        this.subpath(this.getNameCount() - other.getNameCount(), this.getNameCount()).equals(other);
    }

    @Override
    public boolean endsWith(@NotNull String other) {
        return endsWith(getPath(fileSystem, other));
    }

    /**
     * 规范化路径：移除 .（当前目录）和 ..（父目录）等冗余组件
     *
     *
     *
     *
     * @return
     */
    @NotNull
    @Override
    public Path normalize() {
        // 如果是根目录，返回this
        if (pathRepresentation.isRoot()) {
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
        return getParent().resolve(other);
    }

    @NotNull
    @Override
    public Path resolveSibling(@NotNull String other) {
        return getParent().resolve(other);
    }

    @NotNull
    @Override
    public Path relativize(@NotNull Path other) {
        var otherPath = checkPath(other);

        if (this.equals(otherPath)) {
            return from("");
        }

        if (this.isAbsolute() != otherPath.isAbsolute()) {
            throw new IllegalArgumentException("to obtain a relative path both must be absolute or both must be relative");
        }
        if (!Objects.equals(this.bucketName(), otherPath.bucketName())) {
            throw new IllegalArgumentException("cannot relativize S3Paths from different buckets");
        }

        if (this.isEmpty()) {
            return otherPath;
        }

        var nameCount = this.getNameCount();
        var otherNameCount = otherPath.getNameCount();

        var limit = Math.min(nameCount, otherNameCount);
        var differenceCount = getDifferenceCount(otherPath, limit);

        var parentDirCount = nameCount - differenceCount;
        if (differenceCount < otherNameCount) {
            return getRelativePathFromDifference(otherPath, otherNameCount, differenceCount, parentDirCount);
        }

        var relativePath = new char[parentDirCount * 3 - 1];
        var index = 0;
        while (parentDirCount > 0) {
            relativePath[index++] = '.';
            relativePath[index++] = '.';
            if (parentDirCount > 1) {
                relativePath[index++] = '/';
            }
            parentDirCount--;
        }

        return new S3SftpPath(getFileSystem(), new S3SftpPosixLikePathRepresentation(relativePath));
    }

    private S3SftpPath getRelativePathFromDifference(S3SftpPath otherPath, int otherNameCount, int differenceCount, int parentDirCount) {
        Objects.requireNonNull(otherPath);
        var remainingSubPath =  (S3SftpPath) otherPath.subpath(differenceCount, otherNameCount);

        if (parentDirCount == 0) {
            return (S3SftpPath) remainingSubPath;
        }

        // we need to pop up some directories (each of which needs three characters ../) then append the remaining sub-path
        var relativePathSize = parentDirCount * 3 + remainingSubPath.pathRepresentation.toString().length();

        if (otherPath.isEmpty()) {
            relativePathSize--;
        }

        var relativePath = new char[relativePathSize];
        var index = 0;
        while (parentDirCount > 0) {
            relativePath[index++] = '.';
            relativePath[index++] = '.';
            if (otherPath.isEmpty()) {
                if (parentDirCount > 1) {
                    relativePath[index++] = '/';
                }
            } else {
                relativePath[index++] = '/';
            }
            parentDirCount--;
        }
        System.arraycopy(remainingSubPath.pathRepresentation.chars(), 0, relativePath, index,
                remainingSubPath.pathRepresentation.chars().length);

        return new S3SftpPath(getFileSystem(), new S3SftpPosixLikePathRepresentation(relativePath));
    }

    private int getDifferenceCount(Path other, int limit) {
        var i = 0;
        while (i < limit) {
            if (!this.getName(i).equals(other.getName(i))) {
                break;
            }
            i++;
        }
        return i;
    }

    /**
     * touri
     * <p>
     * Path 接口的 toUri() 方法用于将文件路径转换为 URI（Uniform Resource Identifier，统一资源标识符），返回一个 java.net.URI 对象
     *
     * @return
     */
    @NotNull
    @Override
    public URI toUri() {
        Path path = toAbsolutePath().toRealPath(NOFOLLOW_LINKS);
        var elements = path.iterator();

        var uri = new StringBuilder(fileSystem.provider().getScheme() + "://");

        //拼接账户和密码
        uri.append(fileSystem.configuration().accessKey() + ":" + fileSystem.configuration().secretKey() + "@");


        var endpoint = fileSystem.configuration().getEndpoint();
        if (!endpoint.isEmpty()) {
            uri.append(fileSystem.configuration().getEndpoint()).append(PATH_SEPARATOR);
        }
        uri.append(bucketName());
        elements.forEachRemaining(
                (e) -> {
                    var name = e.getFileName().toString();
                    if (name.endsWith(PATH_SEPARATOR)) {
                        name = name.substring(0, name.length() - 1);
                    }
                    uri.append(PATH_SEPARATOR).append(URLEncoder.encode(name, StandardCharsets.UTF_8));
                }
        );
        if (isDirectory()) {
            uri.append(PATH_SEPARATOR);
        }

        return URI.create(uri.toString());
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
    public S3SftpPath toRealPath(@NotNull LinkOption... options) {
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
        throw new UnsupportedOperationException(
                "This method is not yet supported. Please raise a feature request describing your use case"
        );
    }

    @NotNull
    @Override
    public WatchKey register(@NotNull WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException(
                "This method is not yet supported. Please raise a feature request describing your use case"
        );
    }

    private final class S3PathIterator implements Iterator<Path> {
        final boolean isAbsolute;
        final boolean hasTrailingSeparator;
        boolean first;
        private final Iterator<String> delegate;

        private S3PathIterator(Iterator<String> delegate, boolean isAbsolute, boolean hasTrailingSeparator) {
            this.delegate = delegate;
            this.isAbsolute = isAbsolute;
            this.hasTrailingSeparator = hasTrailingSeparator;
            first = true;
        }

        @Override
        public Path next() {
            var pathString = delegate.next();
            if (isAbsolute() && first) {
                first = false;
                pathString = PATH_SEPARATOR + pathString;
                if (!hasNext() && hasTrailingSeparator) {
                    pathString = pathString + PATH_SEPARATOR;
                }
            }

            if (hasNext() || hasTrailingSeparator) {
                pathString = pathString + PATH_SEPARATOR;
            }
            return from(pathString);
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }
    }

    @NotNull
    @Override
    public Iterator<Path> iterator() {
        return  new S3SftpPath.S3PathIterator(pathRepresentation.elements().iterator(), pathRepresentation.isAbsolute(),
                pathRepresentation.hasTrailingSeparator());
    }

    @Override
    public int compareTo(@NotNull Path other) {
        var o = checkPath(other);
        if (o.fileSystem != this.fileSystem) {
            throw new ClassCastException("compared S3 paths must be from the same bucket");
        }
        return this.toRealPath(NOFOLLOW_LINKS).toString().compareTo(
                o.toRealPath(NOFOLLOW_LINKS).toString());
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
     * <p>
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


    /**
     * 重写equals 方法
     *
     * @param obj the object to which this object is to be compared
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof S3SftpPath
                && Objects.equals(((S3SftpPath) obj).bucketName(), this.bucketName())
                && Objects.equals(((S3SftpPath) obj).toRealPath(NOFOLLOW_LINKS).pathRepresentation,
                this.toRealPath(NOFOLLOW_LINKS).pathRepresentation);
    }
}
