package com.gengzi.sftp.nio.util;

import com.gengzi.sftp.nio.S3SftpPath;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class S3Util {


    // s3客户端
    private final S3AsyncClient client;

    // 超时时间
    private final Long timeout;

    // 超时时间类型
    private final TimeUnit timeUnit;

    public S3Util(S3AsyncClient client, Long timeout, TimeUnit timeUnit) {
        this.client = client;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    /**
     * 同步上传文件
     */
    public void uploadLocalFile(S3SftpPath path, Path localFile) throws IOException {
        // S3TransferManager 专为简化 S3 大文件上传、下载、复制等操作设计，底层自动处理分片、断点续传、并发控制等复杂逻辑
        try (S3TransferManager s3TransferManager = S3TransferManager.builder().s3Client(client).build()) {
            CompletableFuture<?> uploadCompletableFuture = s3TransferManager.uploadFile(
                    UploadFileRequest.builder()
                            .putObjectRequest(PutObjectRequest.builder()
                                    .bucket(path.bucketName())
                                    .key(path.getKey())
                                    .contentType(Files.probeContentType(localFile))
                                    .build())
                            .source(localFile)
                            .build()
            ).completionFuture();

            if (timeout != null && timeUnit != null) {
                // 在超时时间内等待获取结果
                uploadCompletableFuture.get(timeout, timeUnit);
            } else {
                // 会阻塞当前线程，直到异步任务执行完成
                uploadCompletableFuture.join();
            }
        } catch (InterruptedException e) {
            // 如果是中断异常，表示有其他线程调用了该线程的 interrupt 方法，会把中断标志改为false，清除中断标志位信息。
            // 调用此方法，重新设置中断标志位为 true
            Thread.currentThread().interrupt();
            throw new IOException("Could not write to path:" + path, e);
        } catch (TimeoutException | ExecutionException e) {
            throw new IOException("Could not write to path:" + path, e);
        }
    }


    /**
     * 同步下载文件
     */
    public void downloadToLocalFile(S3SftpPath path, Path destination) throws IOException {
        try (S3TransferManager s3TransferManager = S3TransferManager.builder().s3Client(client).build()) {
            CompletableFuture downloadCompletableFuture = s3TransferManager.downloadFile(
                    DownloadFileRequest.builder()
                            .getObjectRequest(GetObjectRequest.builder()
                                    .bucket(path.bucketName())
                                    .key(path.getKey())
                                    .build())
                            .destination(destination)
                            .build()
            ).completionFuture();

            if (timeout != null && timeUnit != null) {
                try {
                    downloadCompletableFuture.get(timeout, timeUnit);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Could not open the path:" + path, e);
                } catch (TimeoutException | ExecutionException e) {
                    throw new IOException("Could not open the path:" + path, e);
                }
            } else {
                downloadCompletableFuture.join();
            }
        }


    }


}
