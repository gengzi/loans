package com.gengzi.sftp.s3.client;

import com.gengzi.sftp.s3.client.entity.ObjectHeadResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * s3 对象存储客户端提供者
 */
public interface S3SftpClient<T> {


    /**
     * 创建s3操作客户端
     *
     * @return T
     */
    T createClient();

    /**
     * 从对象存储中获取一个文件内容并返回到ByteBuffer
     *
     * @param bucketName 桶
     * @param key        对象key
     * @param offset     偏移量
     * @param length     长度
     * @return
     */
    CompletableFuture<ByteBuffer> getObject(String bucketName, String key, long offset, long length);


    /**
     * 从对象存储中获取文件内容并保存到本地文件中
     *
     * @param bucketName
     * @param key
     * @param destination
     * @return
     */
    CompletableFuture<?> getObjectAndWriteToLocalFile(String bucketName, String key, Path destination);


    /**
     * 上传本地文件到对象存储中
     *
     * @param bucketName
     * @param key
     * @param localFile
     * @return
     */
    CompletableFuture<?> putObjectByLocalFile(String bucketName, String key, Path localFile);


    /**
     * 获取当前key目录下的所有文件或者子目录名称
     *
     * @param bucketName
     * @param key
     * @return
     */
    CompletableFuture<List> getCurrentKeyDirAllFileNames(String bucketName, String key);

    /**
     * 获取对象的基础元信息（支持对象文件或者对象"目录"）
     *
     * @param bucketName
     * @param key
     * @return 如果无此对象，必须返回 null
     */
    ObjectHeadResponse headObject(String bucketName, String key) throws IOException;


}
