package com.gengzi.sftp.nio;

import software.amazon.awssdk.services.s3.internal.BucketUtils;
import software.amazon.nio.spi.s3.config.S3NioSpiConfiguration;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;


/**
 * 存放配置s3sftp 配置项
 */
public class S3SftpNioSpiConfiguration extends HashMap<String, Object> {


    // s3服务
    private static final String ENDPOINT = "";

    private static final String ACCESS_KEY = "";

    private static final String SECRET_KEY = "";

    private static final Pattern ENDPOINT_REGEXP = Pattern.compile("(\\w[\\w\\-\\.]*)?(:(\\d+))?");

    // 桶
    private String bucketName;


    public S3SftpNioSpiConfiguration() {
        this(new HashMap<>());
    }

    public S3SftpNioSpiConfiguration(Map<String,?> env) {
        Objects.requireNonNull(env);

        // setup defaults
    }

    public String getBucketName() {
        return bucketName;
    }


    public S3SftpNioSpiConfiguration withEndpoint(String endpoint) {
        Objects.requireNonNull(endpoint);
        if(!ENDPOINT_REGEXP.matcher(endpoint).matches()){
            throw new IllegalArgumentException(
                    String.format("endpoint '%s' does not match format host:port where port is a number", endpoint));
        }
        this.put(ENDPOINT, endpoint);
        return this;
    }

    public S3SftpNioSpiConfiguration withBucketName(Object bucket) {
        if (bucketName != null) {
            BucketUtils.isValidDnsBucketName(bucketName, true);
        }
        this.bucketName = bucketName;
        return this;
    }

    public S3SftpNioSpiConfiguration withCredentials(String accessKey, String secretAccessKey) {
        if(Objects.isNull(accessKey) || Objects.isNull(secretAccessKey)){
            throw new IllegalArgumentException("accessKey or secretAccessKey can not be null");
        }
        put(ACCESS_KEY, accessKey);
        put(SECRET_KEY, secretAccessKey);
        return this;
    }

    public URI endpointUri() {
        return URI.create(get(ENDPOINT).toString());
    }

    public String accessKey() {
    }

    public String secretKey() {
    }
}
