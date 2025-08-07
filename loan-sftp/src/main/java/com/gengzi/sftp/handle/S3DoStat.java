package com.gengzi.sftp.handle;


import com.gengzi.sftp.config.MinIoClientConfig;
import com.gengzi.sftp.util.SpringContextUtil;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class S3DoStat {


    public static  NavigableMap<String, Object> doStat(int id, String path, int flags) {
        // 处理只存在路径，不存在目标文件的情况
        Path s3Path = Paths.get(path);
        Path fileName = s3Path.getFileName();
        if (fileName != null && fileName.toString().contains(".")) {
            // 可能包含一个文件
        } else {
            NavigableMap<String, Object> attrs = new TreeMap<>();
            ArrayList<PosixFilePermission> posixFilePermissions = new ArrayList<>();
            posixFilePermissions.add(PosixFilePermission.OWNER_READ);
            posixFilePermissions.add(PosixFilePermission.OWNER_WRITE);
            posixFilePermissions.add(PosixFilePermission.OTHERS_READ);
            posixFilePermissions.add(PosixFilePermission.OTHERS_WRITE);
            posixFilePermissions.add(PosixFilePermission.GROUP_READ);
            posixFilePermissions.add(PosixFilePermission.GROUP_WRITE);
            attrs.put("isDirectory", true);
            attrs.put("isRegularFile", true);
            attrs.put("isSymbolicLink", false);
            attrs.put("size", 0L);
            attrs.put("permissions", posixFilePermissions);
            attrs.put("owner", "admin");
            return attrs;
        }





        MinioClient s3Client = (MinioClient) SpringContextUtil.getBean("s3Client");
        MinIoClientConfig config = SpringContextUtil.getBean(MinIoClientConfig.class);
        try {
            StatObjectResponse statObjectResponse = s3Client.statObject(
                    StatObjectArgs.builder()
                            .bucket(config.getDefaultBucketName())
                            .object(path)
                            .build());
            NavigableMap<String, Object> attrs = new TreeMap<>();
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
            attrs.put("size", statObjectResponse.size());
            attrs.put("lastModifiedTime", FileTime.from(statObjectResponse.lastModified().toInstant().toEpochMilli(), TimeUnit.MILLISECONDS));
            attrs.put("permissions", posixFilePermissions);
            attrs.put("owner", "admin");
            return attrs;

        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        }
    }



}
