package com.gengzi.sftp.stream;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 *
 * 构造s3文件流
 * @author: gengzi
 * @date: 2020/7/31
 */
public class S3DirectoryStream implements DirectoryStream<Path> {
    private List<Path> paths;

    public S3DirectoryStream(List<Path> paths) {
        this.paths = paths;
    }


    @Override
    public Iterator<Path> iterator() {
        return paths.iterator();
    }

    @Override
    public void close() throws IOException {

    }
}
