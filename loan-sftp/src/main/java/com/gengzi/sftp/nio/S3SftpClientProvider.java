package com.gengzi.sftp.nio;


import io.netty.util.concurrent.DefaultThreadFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

import java.net.URI;
import java.time.Duration;

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
//        asyncClientBuilder.endpointOverride(configuration.endpointUri());
//        asyncClientBuilder.region(Region.US_EAST_1);
//        asyncClientBuilder.credentialsProvider(StaticCredentialsProvider.create(
//                AwsBasicCredentials.create(configuration.accessKey(), configuration.secretKey())
//        ));
//        asyncClientBuilder.requestChecksumCalculation(RequestChecksumCalculation.WHEN_SUPPORTED);
//        asyncClientBuilder.responseChecksumValidation(ResponseChecksumValidation.WHEN_SUPPORTED);
//        return asyncClientBuilder.build();



        return S3AsyncClient.builder()
                .endpointOverride(configuration.endpointUri()) // MinIO 服务地址
                .region(Region.US_EAST_1) // MinIO 通常使用自定义区域或 us-east-1
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(configuration.accessKey(), configuration.secretKey())
                ))
                .serviceConfiguration(service -> service
                        .pathStyleAccessEnabled(true) // MinIO 推荐启用路径风格
                )
                .build();

    }
}
