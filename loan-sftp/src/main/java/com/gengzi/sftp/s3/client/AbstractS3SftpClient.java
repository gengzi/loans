package com.gengzi.sftp.s3.client;

import com.gengzi.sftp.nio.S3SftpNioSpiConfiguration;

public abstract class AbstractS3SftpClient implements S3SftpClient{


    public S3SftpNioSpiConfiguration configuration;


    public AbstractS3SftpClient(S3SftpNioSpiConfiguration s3SftpNioSpiConfiguration) {
        this.configuration = s3SftpNioSpiConfiguration;
    }
}
