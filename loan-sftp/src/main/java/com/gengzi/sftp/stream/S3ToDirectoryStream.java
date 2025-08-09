package com.gengzi.sftp.stream;

import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 将 S3 对象列表转换为模拟的 DirectoryStream<Path>
 */
public class S3ToDirectoryStream implements DirectoryStream<Path> {
    // 虚拟根路径（用于映射 S3 对象为本地风格的 Path）
    private final Path virtualRoot;
    // S3 对象列表
    private final List<S3Object> s3Objects;
    // 迭代器
    private final Iterator<S3Object> s3Iterator;

    /**
     * 构造方法
     *
     * @param s3Objects   S3 对象列表
     * @param virtualRoot 虚拟根路径（如 "/s3-mount"）
     */
    public S3ToDirectoryStream(List<S3Object> s3Objects, Path virtualRoot) {
        this.s3Objects = s3Objects;
        this.virtualRoot = virtualRoot;
        this.s3Iterator = s3Objects.iterator();
    }


    /**
     * 迭代器：将 S3Object 转换为虚拟 Path
     */
    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            @Override
            public boolean hasNext() {
                return s3Iterator.hasNext();
            }

            @Override
            public Path next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                S3Object s3Object = s3Iterator.next();
                // 将 S3 对象的 key 转换为基于虚拟根的 Path
                return virtualRoot.resolve(s3Object.key());
            }
        };
    }

    /**
     * 关闭流（S3 无资源需要释放，仅作空实现）
     */
    @Override
    public void close() throws IOException {
        // 无需实际关闭资源，因为 S3Object 是内存中的对象列表
    }
}