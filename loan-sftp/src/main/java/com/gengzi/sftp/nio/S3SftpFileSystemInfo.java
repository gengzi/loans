package com.gengzi.sftp.nio;

import software.amazon.awssdk.services.s3.internal.BucketUtils;

import java.net.URI;

/**
 * 解析URI
 */
public class S3SftpFileSystemInfo {


    private static final String URI_PATH_SEPARATOR = "/";
    // 账户
    private String accessKey;
    // 秘钥
    private String accessSecret;
    // s3地址
    private String endpoint;
    // bucket
    private String bucket;

    private String key;


    public S3SftpFileSystemInfo(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri can not be null");
        }

        final var userInfo = uri.getUserInfo();

        if (userInfo != null) {
            var pos = userInfo.indexOf(':');
            accessKey = (pos < 0) ? userInfo : userInfo.substring(0, pos);
            accessSecret = (pos < 0) ? null : userInfo.substring(pos + 1);
        }

        endpoint = uri.getHost();
        if (uri.getPort() > 0) {
            endpoint += ":" + uri.getPort();
        }
        bucket = uri.getPath().split(URI_PATH_SEPARATOR)[1];

        BucketUtils.isValidDnsBucketName(bucket, true);

        key = endpoint + '/' + bucket;
        if (accessKey != null) {
            key = accessKey + '@' + key;
        }

    }


    public String key() {
        return key;
    }

    public String endpoint() {
        return endpoint;
    }

    public String accessKey() {
        return accessKey;
    }
    public String accessSecret() {
        return accessSecret;
    }

    public String bucket() {
        return bucket;
    }
}
