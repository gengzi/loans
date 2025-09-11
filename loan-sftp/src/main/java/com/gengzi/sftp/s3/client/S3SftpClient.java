package com.gengzi.sftp.s3.client;

import java.nio.ByteBuffer;

/**
 * s3 对象存储客户端提供者
 */
public interface S3SftpClient<T> {



    T createClient();

    ByteBuffer getObject();







}
