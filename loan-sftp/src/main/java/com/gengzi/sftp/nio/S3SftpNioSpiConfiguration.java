package com.gengzi.sftp.nio;

import software.amazon.awssdk.services.s3.internal.BucketUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


/**
 * 存放配置s3sftp配置项
 */
public class S3SftpNioSpiConfiguration extends HashMap<String, Object> {

    private static final String ENDPOINT = "endpoint";

    private static final String ACCESS_KEY = "accessKey";

    private static final String SECRET_KEY = "secretKey";

    private static final Pattern ENDPOINT_REGEXP = Pattern.compile("(\\w[\\w\\-\\.]*)?(:(\\d+))?");

    private static final String TIME_OUT = "timeout";
    private static Long TIME_OUT_VAL = 60L;

    private static final String TIME_OUT_UNIT = "timeoutUnit";
    private static TimeUnit TIME_OUT_UNIT_VAL = TimeUnit.MILLISECONDS;

    // 桶
    private String bucketName;


    public S3SftpNioSpiConfiguration() {
        this(new HashMap<>());
        put(TIME_OUT, TIME_OUT_VAL);
        put(TIME_OUT_UNIT, TIME_OUT_UNIT_VAL);
    }

    public S3SftpNioSpiConfiguration(Map<String, ?> env) {
        Objects.requireNonNull(env);

        // setup defaults
    }

    public String getBucketName() {
        return bucketName;
    }


    public S3SftpNioSpiConfiguration withEndpoint(String endpoint) {
        Objects.requireNonNull(endpoint);
        if (!ENDPOINT_REGEXP.matcher(endpoint).matches()) {
            throw new IllegalArgumentException(
                    String.format("endpoint '%s' does not match format host:port where port is a number", endpoint));
        }
        this.put(ENDPOINT, endpoint);
        return this;
    }

    public S3SftpNioSpiConfiguration withBucketName(String bucket) {
        if (bucket != null) {
            BucketUtils.isValidDnsBucketName(bucket, true);
        }
        this.bucketName = bucket;
        return this;
    }

    public S3SftpNioSpiConfiguration withCredentials(String accessKey, String secretAccessKey) {
        if (Objects.isNull(accessKey) || Objects.isNull(secretAccessKey)) {
            throw new IllegalArgumentException("accessKey or secretAccessKey can not be null");
        }
        put(ACCESS_KEY, accessKey);
        put(SECRET_KEY, secretAccessKey);
        return this;
    }


    public String getEndpoint() {
        return get(ENDPOINT).toString();
    }

    public URI endpointUri() {
        return URI.create("http://" + get(ENDPOINT).toString());
    }

    public String accessKey() {
        return get(ACCESS_KEY).toString();
    }

    public String secretKey() {
        return get(SECRET_KEY).toString();
    }


    public Long timeout() {
        return (Long) get(TIME_OUT);
    }

    public TimeUnit timeoutUnit() {
        return (TimeUnit) get(TIME_OUT_UNIT);
    }
}
