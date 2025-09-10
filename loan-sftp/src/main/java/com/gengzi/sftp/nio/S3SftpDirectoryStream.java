package com.gengzi.sftp.nio;

import com.gengzi.sftp.nio.constans.Constants;
import io.reactivex.rxjava3.core.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;


public class S3SftpDirectoryStream implements DirectoryStream {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private String bucketName;

    private S3SftpFileSystem fileSystem;
    // 目录路径
    private String path;

    private DirectoryStream.Filter<? super Path> filter;

    private Iterator<Path> dirs;


    public S3SftpDirectoryStream(S3SftpFileSystem fileSystem,String bucketName,  String path,DirectoryStream.Filter<? super Path> filter) {
        this.path = path;
        this.bucketName = bucketName;
        this.fileSystem = fileSystem;
        this.filter = filter;

        ListObjectsV2Publisher listObjectsV2Publisher = fileSystem.client().listObjectsV2Paginator(req -> req
                .bucket(bucketName)
                .prefix(path)
                .delimiter(Constants.PATH_SEPARATOR));

        dirs = pathIteratorForPublisher(listObjectsV2Publisher, fileSystem, path, filter);


    }

    Iterator<Path>  pathIteratorForPublisher(ListObjectsV2Publisher listObjectsV2Publisher, S3SftpFileSystem fs, String finalDirName,  DirectoryStream.Filter<? super Path> filter){
        final Publisher<String> prefixPublisher =
                listObjectsV2Publisher.commonPrefixes().map(CommonPrefix::prefix);
        final Publisher<String> keysPublisher =
                listObjectsV2Publisher.contents().map(S3Object::key);
        return Flowable.concat(prefixPublisher, keysPublisher)
                .map(fs::getPath)
                .filter(path -> !isEqualToParent(finalDirName, path))  // including the parent will induce loops
                .filter(path -> tryAccept(filter, path))
                .blockingIterable()
                .iterator();

    }

    private static boolean isEqualToParent(String finalDirName, Path p) {
        return ((S3SftpPath) p).getKey().equals(finalDirName);
    }

    private boolean tryAccept(DirectoryStream.Filter<? super Path> filter, Path path) {
        try {
            return filter.accept(path);
        } catch (IOException e) {
            logger.warn("An IOException was thrown while filtering the path: {}." +
                    " Set log level to debug to show stack trace", path);
            logger.debug(e.getMessage(), e);
            return false;
        }
    }


    /**
     * Returns the iterator associated with this {@code DirectoryStream}.
     *
     * @return the iterator associated with this {@code DirectoryStream}
     * @throws IllegalStateException if this directory stream is closed or the iterator has already
     *                               been returned
     */
    @Override
    public Iterator iterator() {
        return dirs;
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {

    }
}
