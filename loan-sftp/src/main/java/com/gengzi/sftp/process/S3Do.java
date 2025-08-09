package com.gengzi.sftp.process;

import com.gengzi.sftp.config.AmazonS3Config;
import com.gengzi.sftp.util.SpringContextUtil;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;

public class S3Do {

    private final S3Client s3Client;
    private final AmazonS3Config amazonS3Config;

    public S3Do() {
        this.s3Client = AmazonS3Config.getS3Client();;
        this.amazonS3Config = SpringContextUtil.getBean(AmazonS3Config.class);
    }

    public AmazonS3Config getAmazonS3Config() {
        return amazonS3Config;
    }

    /**
     * 检查S3中是否存在指定的对象（文件）
     * @param bucketName 存储桶名称
     * @param objectKey 对象完整路径（例如："documents/report.pdf"）
     * @return true=存在，false=不存在
     */
    public boolean doesObjectExist(String bucketName, String objectKey) {
        try {
            // 发送HEAD请求检查对象是否存在
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            // 如果没有抛出异常，说明对象存在
            HeadObjectResponse response = s3Client.headObject(request);
            return true;
        } catch (S3Exception e) {
            // 捕获404错误（对象不存在）
            if (e.statusCode() == 404) {
                return false;
            }
            // 其他错误（如权限不足）抛出异常
            throw e;
        }
    }

    /**
     * 检查S3中是否存在指定的目录前缀（模拟的目录）
     * @param bucketName 存储桶名称
     * @param directoryPrefix 目录前缀（例如："documents/reports/"，注意末尾的斜杠）
     * @return true=存在（包含至少一个对象），false=不存在
     */
    public boolean doesDirectoryExist(String bucketName, String directoryPrefix) {
        // 确保目录前缀以斜杠结尾（符合S3的目录命名习惯）
        String normalizedPrefix = directoryPrefix.endsWith("/") ? directoryPrefix : directoryPrefix + "/";
        // 移除前缀开头的 /
        if(normalizedPrefix != null && normalizedPrefix.startsWith("/")){
            normalizedPrefix = normalizedPrefix.substring(1);
        }
        try {
            // 列出前缀下的第一个对象
            ListObjectsV2Response response = null;
            if("/".equals(normalizedPrefix)){
                ListObjectsV2Request rootRequest = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .delimiter("/")
                        .maxKeys(1) // 只需要检查是否有至少一个对象
                        .build();
                 response = s3Client.listObjectsV2(rootRequest);
            }else{
                ListObjectsV2Request prefixRequest = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(normalizedPrefix)
                        .delimiter("/")
                        .maxKeys(1) // 只需要检查是否有至少一个对象
                        .build();
                response = s3Client.listObjectsV2(prefixRequest);
            }


            // 如果有对象或公共前缀，说明目录存在
            return !response.contents().isEmpty() || !response.commonPrefixes().isEmpty();
        } catch (S3Exception e) {
            // 处理错误（如权限不足）
            throw e;
        }
    }


    /**
     * 获取指定目录下的所有文件信息（支持分页）
     * @param directoryPath 目录路径（例如："docs/reports/"，注意末尾的斜杠）
     * @return 所有文件的信息列表
     */
    public List<S3Object> listFilesInDirectory(String bucketName,String directoryPath) {
        // 确保目录前缀以斜杠结尾（符合S3的目录命名习惯）
        String normalizedPrefix = directoryPath.endsWith("/") ? directoryPath : directoryPath + "/";

        List<S3Object> allObjects = new ArrayList<>();
        String continuationToken = null;

        do {
            // 构建请求
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(normalizedPrefix) // 指定目录前缀（模拟目录）
                    .delimiter("/") // 可选：只列出当前目录下的对象，不包含子目录内容
                    .continuationToken(continuationToken) // 分页令牌
                    .maxKeys(1000) // 每次请求最大返回数量（最大1000）
                    .build();

            // 执行请求
            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            // 添加当前页的对象到结果列表
            allObjects.addAll(response.contents());

            // 获取下一页令牌
            continuationToken = response.nextContinuationToken();

        } while (continuationToken != null); // 直到没有更多结果

        return allObjects;
    }

}
