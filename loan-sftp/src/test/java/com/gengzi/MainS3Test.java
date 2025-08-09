//package com.gengzi;
//
//import com.amazonaws.ClientConfiguration;
//import com.amazonaws.auth.AWSStaticCredentialsProvider;
//import com.amazonaws.auth.BasicAWSCredentials;
//import com.amazonaws.client.builder.AwsClientBuilder;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import com.amazonaws.services.s3.model.*;
//import io.minio.errors.*;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class MainS3Test {
//
//    public static void main(String[] args) throws Exception {
//
//
//        // 配置MinIO服务端点
//        AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder.EndpointConfiguration(
//                "http://127.0.0.1:9000", "us-east-1");
//
//        // 配置访问凭证
//        BasicAWSCredentials credentials = new BasicAWSCredentials("minioadmin", "minioadmin");
//
//        // 构建客户端（禁用签名区域验证，适应MinIO私有部署）
//        ClientConfiguration clientConfig = new ClientConfiguration();
//        clientConfig.setSignerOverride("AWSS3V4SignerType"); // 使用V4签名算法
//
//
//        AmazonS3 clientS3Client = AmazonS3ClientBuilder.standard()
//                .withEndpointConfiguration(endpointConfig)
//                .withCredentials(new AWSStaticCredentialsProvider(credentials))
//                .withClientConfiguration(clientConfig)
//                .enablePathStyleAccess() // 启用路径风格访问（MinIO推荐）
//                .build();
//
//
//        // 要追加的内容
//        String contentToAppend = "\nThis is appended content. haha";
//
//
//        // 执行追加操作
//        appendToS3Object(clientS3Client,"image", "/s3/file/5.txt", contentToAppend);
//
//
//    }
//
//
//    /**
//     * 向S3对象追加内容
//     */
//    public static void appendToS3Object(AmazonS3 s3Client, String bucketName, String objectKey, String content) throws Exception {
//        // 检查对象是否存在
//        boolean objectExists = s3Client.doesObjectExist(bucketName, objectKey);
//        List<PartETag> partETags = new ArrayList<>();
//        String uploadId = null;
//
//        try {
//            // 初始化分段上传
//            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, objectKey);
//            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);
//            uploadId = initResponse.getUploadId();
//
//            if (objectExists) {
//                // 如果对象存在，先上传原有内容作为第一个分段
//                S3Object existingObject = s3Client.getObject(bucketName, objectKey);
//                InputStream existingContent = existingObject.getObjectContent();
//
//                // 上传原有内容
//                UploadPartRequest uploadPartRequest = new UploadPartRequest()
//                        .withBucketName(bucketName)
//                        .withKey(objectKey)
//                        .withUploadId(uploadId)
//                        .withPartNumber(1)
//                        .withInputStream(existingContent)
//                        .withPartSize(existingObject.getObjectMetadata().getContentLength());
//
//                UploadPartResult uploadResult = s3Client.uploadPart(uploadPartRequest);
//                partETags.add(uploadResult.getPartETag());
//                existingContent.close();
//            }
//
//            // 上传要追加的内容作为新的分段
//            int newPartNumber = objectExists ? 2 : 1;
//            InputStream newContent = new ByteArrayInputStream(content.getBytes());
//
//            UploadPartRequest appendPartRequest = new UploadPartRequest()
//                    .withBucketName(bucketName)
//                    .withKey(objectKey)
//                    .withUploadId(uploadId)
//                    .withPartNumber(newPartNumber)
//                    .withInputStream(newContent)
//                    .withPartSize(content.getBytes().length);
//
//            UploadPartResult appendResult = s3Client.uploadPart(appendPartRequest);
//            partETags.add(appendResult.getPartETag());
//            newContent.close();
//
//            // 完成分段上传，合并所有分段
//            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
//                    bucketName, objectKey, uploadId, partETags);
//            s3Client.completeMultipartUpload(completeRequest);
//
//        } catch (Exception e) {
//            // 如果发生错误，中止分段上传
//            if (uploadId != null) {
//                s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, objectKey, uploadId));
//            }
//            throw e;
//        }
//    }
//}
