package com.gengzi.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.IOException;
import java.net.URL;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class S3ClientUtils {

    private static final Logger logger = LoggerFactory.getLogger(S3ClientUtils.class);
    @Autowired
    @Qualifier("s3Client")
    private S3AsyncClient s3Client;


    @Autowired
    @Qualifier("s3Presigner")
    private S3Presigner presigner;

    public URL generatePresignedUrl(String bucketName, String objectKey) {
        try {
            logger.debug("generatePresignedUrl bucketName:{},objectKey:{}", bucketName, objectKey);
            // 构建获取对象的请求
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            // 构建预签名请求
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofHours(24))
                    .build();
            // 生成预签名URL
            URL url = presigner.presignGetObject(presignRequest).url();
            logger.debug("文件下载地址: " + url.toString());
            return url;
        } catch (Exception e) {
            logger.error("获取下载地址失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    public void putObjectByContentBytes(String bucketName, String key, byte[] bytes, String contentType)  {
        logger.info("putObjectByLocalFile bucketName:{},key:{}", bucketName, key);
        try (S3TransferManager s3TransferManager = S3TransferManager.builder().s3Client(this.s3Client).build()) {
            CompletableFuture<CompletedUpload> completedUploadCompletableFuture = s3TransferManager.upload(
                    UploadRequest.builder()
                            .putObjectRequest(req -> req
                                    .bucket(bucketName)
                                    .key(key)
                                    .contentType(contentType)) // 根据内容类型调整
                            .requestBody(AsyncRequestBody.fromBytes(bytes))
                            .build()
            ).completionFuture();
            // 会阻塞当前线程，直到异步任务执行完成
            completedUploadCompletableFuture.join();
        }
    }

    public HeadObjectResponse headObject(String bucketName, String key){
        logger.debug("headObject bucketName:{},key:{} ", bucketName, key);
        try {
            return getObjectAttributes(bucketName, key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HeadObjectResponse getObjectAttributes(String bucketName, String key) throws IOException {
        try {
            return this.s3Client.headObject(req -> req
                    .bucket(bucketName)
                    .key(key)
            ).get(20, TimeUnit.SECONDS);
        } catch (NoSuchKeyException e) {
            return null;
        } catch (ExecutionException e) {
            String errMsg = String.format("path: %s getFileAttributes error!!! req s3 server :%s",
                    key, e.getCause().toString());
            logger.error(errMsg);
            throw new IOException(errMsg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (TimeoutException e) {
            throw new IOException("path " + key + "getFileAttributes timeout ", e);
        }
    }


}
