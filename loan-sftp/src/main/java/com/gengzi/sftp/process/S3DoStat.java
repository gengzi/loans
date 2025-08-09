package com.gengzi.sftp.process;


import com.gengzi.sftp.config.AmazonS3Config;
import com.gengzi.sftp.util.SpringContextUtil;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class S3DoStat {


    public static NavigableMap<String, Object> doStat(int id, String path, int flags) {
        // 处理只存在路径，不存在目标文件的情况
        Path s3Path = Paths.get(path);
        Path fileName = s3Path.getFileName();
        NavigableMap<String, Object> attrs = new TreeMap<>();
        if (fileName != null && fileName.toString().contains(".")) {
            // 可能包含一个文件
        } else {
            ArrayList<PosixFilePermission> posixFilePermissions = new ArrayList<>();
            posixFilePermissions.add(PosixFilePermission.OWNER_READ);
            posixFilePermissions.add(PosixFilePermission.OWNER_WRITE);
            posixFilePermissions.add(PosixFilePermission.OTHERS_READ);
            posixFilePermissions.add(PosixFilePermission.OTHERS_WRITE);
            posixFilePermissions.add(PosixFilePermission.GROUP_READ);
            posixFilePermissions.add(PosixFilePermission.GROUP_WRITE);
            attrs.put("isDirectory", true);
            attrs.put("isRegularFile", false);
            attrs.put("isSymbolicLink", false);
            attrs.put("size", 4096L);
            attrs.put("permissions", posixFilePermissions);
            attrs.put("owner", "admin");
            return attrs;
        }

        AmazonS3Config config = SpringContextUtil.getBean(AmazonS3Config.class);
        S3Client s3Client = (S3Client) SpringContextUtil.getBean("AmazonS3Client");
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(config.getDefaultBucketName())
                .key(path)
                .build();
        HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

        ArrayList<PosixFilePermission> posixFilePermissions = new ArrayList<>();
        posixFilePermissions.add(PosixFilePermission.OWNER_READ);
        posixFilePermissions.add(PosixFilePermission.OWNER_WRITE);
        posixFilePermissions.add(PosixFilePermission.OTHERS_READ);
        posixFilePermissions.add(PosixFilePermission.OTHERS_WRITE);
        posixFilePermissions.add(PosixFilePermission.GROUP_READ);
        posixFilePermissions.add(PosixFilePermission.GROUP_WRITE);
        attrs.put("isDirectory", false);
        attrs.put("isRegularFile", true);
        attrs.put("isSymbolicLink", false);
        attrs.put("size", headObjectResponse.contentLength());
        attrs.put("lastModifiedTime", FileTime.from(headObjectResponse.lastModified().toEpochMilli(), TimeUnit.MILLISECONDS));
        attrs.put("permissions", posixFilePermissions);
        attrs.put("owner", "admin");
        return attrs;

    }


}
