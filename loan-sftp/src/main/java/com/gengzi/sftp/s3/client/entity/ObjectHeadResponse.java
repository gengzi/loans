package com.gengzi.sftp.s3.client.entity;


import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * 对象元信息响应类
 *
 */
public class ObjectHeadResponse {


    private FileTime lastModifiedTime;
    private Long size;
    private Object eTag;
    private boolean isDirectory;
    private boolean isRegularFile;
    // 是否为空目录，目录下没有任何文件和子目录
    private boolean isEmptyDirectory;

    public ObjectHeadResponse(FileTime lastModifiedTime,
                              Long size,
                              Object eTag,
                              boolean isDirectory,
                              boolean isRegularFile,
                              boolean isEmptyDirectory) {
        this.lastModifiedTime = lastModifiedTime;
        this.size = size;
        this.eTag = eTag;
        this.isDirectory = isDirectory;
        this.isRegularFile = isRegularFile;
        this.isEmptyDirectory = isEmptyDirectory;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public Long getSize() {
        return size;
    }

    public Object geteTag() {
        return eTag;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isRegularFile() {
        return isRegularFile;
    }

    public boolean isEmptyDirectory() {
        return isEmptyDirectory;
    }
}
