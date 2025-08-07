package com.gengzi.sftp.config;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmazonS3Config {

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

    // 区域（MinIO可任意指定，如"us-east-1"）
    private static final String REGION = "us-east-1";

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
    public AmazonS3 getMinioS3Client(MinioClient s3Client) {
        // 配置MinIO服务端点
        AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder.EndpointConfiguration(
                endpoint, REGION);
        // 配置访问凭证
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        // 构建客户端（禁用签名区域验证，适应MinIO私有部署）
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setSignerOverride("AWSS3V4SignerType"); // 使用V4签名算法

        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfig)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withClientConfiguration(clientConfig)
                .enablePathStyleAccess() // 启用路径风格访问（MinIO推荐）
                .build();
        // 默认开启版本控制
        setBucketVersioning(client, defaultBucketName);
        return client;
    }


    /**
     * 启用版本控制
     *
     * @param s3Client
     * @param BUCKET_NAME
     * @return
     */
    public static String setBucketVersioning(AmazonS3 s3Client, String BUCKET_NAME) {
        // 创建版本控制配置（启用版本控制）
        BucketVersioningConfiguration versioningConfig = new BucketVersioningConfiguration()
                .withStatus(BucketVersioningConfiguration.ENABLED);
        // 应用配置到存储桶
        SetBucketVersioningConfigurationRequest request = new SetBucketVersioningConfigurationRequest(
                BUCKET_NAME, versioningConfig);
        s3Client.setBucketVersioningConfiguration(request);
        BucketVersioningConfiguration currentConfig = s3Client.getBucketVersioningConfiguration(BUCKET_NAME);
        return currentConfig.getStatus();
    }




}
