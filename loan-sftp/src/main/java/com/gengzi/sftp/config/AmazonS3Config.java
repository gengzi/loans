package com.gengzi.sftp.config;


import io.minio.MinioClient;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.Duration;

@Configuration
public class AmazonS3Config {

    // 区域（MinIO可任意指定，如"us-east-1"）
    private static final String REGION = "us-east-1";
    @Value("${s3.endpoint}")
    private String endpoint;
    @Value("${s3.accessKey}")
    private String accessKey;
    @Value("${s3.secretKey}")
    private String secretKey;
    @Value("${s3.defaultBucketName}")
    private String defaultBucketName;
    @Value("${s3.localPath}")
    private String localPath;
    @Autowired
    private AsyncNettyPoolConfig asyncNettyPoolConfig;

    public String getLocalPath() {
        return localPath;
    }

    public String getDefaultBucketName() {
        return defaultBucketName;
    }

    /**
     * 创建MinIO的S3客户端
     */
    @Bean("AmazonS3Client")
    public S3Client minioClient(MinioClient s3Client) {
        // 配置访问凭证
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        // 构建S3客户端
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(endpoint)) // 设置MinIO服务端点
                .region(Region.of(REGION)) // 设置区域
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(config -> config
                        .pathStyleAccessEnabled(true) // 启用路径风格访问（MinIO推荐）
                )
                .build();
        return client;
    }


    /**
     * 配置并创建适用于 MinIO 的 S3AsyncClient 实例
     */
    @Bean("AsyncAmazonS3Client")
    public S3AsyncClient minioAsyncClient() {

        // 1. 配置Netty事件循环组（线程池）
        SdkEventLoopGroup eventLoopGroup = SdkEventLoopGroup.builder()
                .numberOfThreads(asyncNettyPoolConfig.getCoreThreads())
                .threadFactory(new DefaultThreadFactory("s3-async-")) // 线程名前缀
                .build();


        // 2. 配置异步HTTP客户端
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .eventLoopGroup(eventLoopGroup) // 关联自定义线程池
                .maxConcurrency(asyncNettyPoolConfig.getMaxThreads()) // 最大并发连接
                .connectionTimeout(Duration.ofSeconds(asyncNettyPoolConfig.getConnectionTimeout())) // 连接超时
                .maxPendingConnectionAcquires(1000) // 最大等待连接获取的请求数
                .build();


        return S3AsyncClient.builder()
                .endpointOverride(URI.create(endpoint)) // MinIO 服务地址
                .region(Region.of(REGION)) // MinIO 通常使用自定义区域或 us-east-1
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .httpClient(httpClient)
                .serviceConfiguration(service -> service
                        .pathStyleAccessEnabled(true) // MinIO 推荐启用路径风格
                )
                .build();
    }

}
