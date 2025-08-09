package com.gengzi.sftp.nio;


import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

import java.net.URI;

/**
 * 创建s3 客户端工厂
 */
public class S3SftpClientProvider {


    protected final S3SftpNioSpiConfiguration configuration;

    protected S3CrtAsyncClientBuilder asyncClientBuilder =
            S3AsyncClient.crtBuilder()
                    .crossRegionAccessEnabled(true);
    /**
     * 根据配置创建s3客户端
     * @param config
     */
    public S3SftpClientProvider(S3SftpNioSpiConfiguration config) {
        this.configuration = config;
    }


    public S3AsyncClient generateClient(String bucketName) {
        //TODO 创建s3客户端 ,先看缓存中是否已经创建好了，如果创建好了直接获取
        asyncClientBuilder.endpointOverride(configuration.endpointUri());
        asyncClientBuilder.region(Region.of(""));
        asyncClientBuilder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(configuration.accessKey(), configuration.secretKey())
        ));
        return asyncClientBuilder.build();

    }
}
