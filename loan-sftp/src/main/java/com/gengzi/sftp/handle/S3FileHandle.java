package com.gengzi.sftp.handle;

import com.gengzi.sftp.config.AmazonS3Config;
import com.gengzi.sftp.util.SpringContextUtil;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.server.Handle;
import org.apache.sshd.sftp.server.SftpSubsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.sshd.sftp.server.FileHandle.getOpenOptions;


/**
 * S3FileHandle
 */
public class S3FileHandle extends Handle {


    // 初始化日志对象（类名作为日志名，便于定位）
    private static final Logger logger = LoggerFactory.getLogger(S3FileHandle.class);

    private final int access;
    private final Set<StandardOpenOption> openOptions;
    private S3Client s3Client;
    private S3AsyncClient s3AsyncClient;
    private String defaultBucketName;
    // 存储对象的 路径+文件名称
    private String objectName;
    // 本地临时目录
    private String localPath;
    /**
     * 临时文件
     */
    private String tempFile;

    private Boolean fileIsUploaded = true;

    public S3FileHandle(SftpSubsystem subsystem, Path file, int flags, String handle, int access) {
        super(subsystem, file, handle);
        Set<StandardOpenOption> options = getOpenOptions(flags, access);
        // Java cannot do READ | WRITE | APPEND; it throws an IllegalArgumentException "READ+APPEND not allowed". So
        // just open READ | WRITE, and use the ACE4_APPEND_DATA access flag to indicate that we need to handle "append"
        // mode ourselves. ACE4_APPEND_DATA should only have an effect if the file is indeed opened for APPEND mode.
        int desiredAccess = access & ~SftpConstants.ACE4_APPEND_DATA;
        if (options.contains(StandardOpenOption.APPEND)) {
            desiredAccess |= SftpConstants.ACE4_APPEND_DATA | SftpConstants.ACE4_WRITE_DATA
                    | SftpConstants.ACE4_WRITE_ATTRIBUTES;
            options.add(StandardOpenOption.WRITE);
            options.remove(StandardOpenOption.APPEND);
        }
        this.access = desiredAccess;
        this.openOptions = Collections.unmodifiableSet(options);
        AmazonS3Config config = SpringContextUtil.getBean(AmazonS3Config.class);
        this.s3Client = (S3Client) SpringContextUtil.getBean("AmazonS3Client");
        this.s3AsyncClient = (S3AsyncClient) SpringContextUtil.getBean("AsyncAmazonS3Client");
        defaultBucketName = config.getDefaultBucketName();
        objectName = file.toString();
        localPath = config.getLocalPath();
    }

    /**
     * 准备文件通道
     *
     * @param filePath 文件路径
     * @return SeekableByteChannel 实例
     * @throws IOException IO异常
     */
    public static SeekableByteChannel prepareChannel(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        // 创建文件对象
        File file = new File(filePath);

        // 判断文件是否存在，不存在则创建
        if (!file.exists()) {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            // 创建新文件
            file.createNewFile();
        }

        // 打开选项：不存在则创建，支持读写，不自动追加
        EnumSet<StandardOpenOption> options = EnumSet.of(
                StandardOpenOption.CREATE,  // 不存在则创建文件
                StandardOpenOption.READ,    // 支持读取
                StandardOpenOption.WRITE    // 支持写入
        );
        // 获取通道
        return Files.newByteChannel(path, options);
    }

    public Boolean getFileIsUploaded() {
        return fileIsUploaded;
    }

    public int getAccessMask() {
        return access;
    }

    /**
     * 读取文件
     *
     * @param data   byte 数据存放集合
     * @param doff   data[] 数组的数据偏移，从这个位置开始存放数据
     * @param length 长度，每次读取长度
     * @param offset 数据偏移  从这个位置开始读取数据
     * @param eof    文件是否结束
     * @return
     */
    public int read(byte[] data, int doff, int length, long offset, AtomicReference<Boolean> eof) {
        HeadObjectResponse objectMetadata = getObjectMetadata();
        //  判断文件大小小于或者等于 offset，说明文件已经读取完毕了。
        if (objectMetadata.contentLength() <= offset) {
            eof.set(true);
            return -1;
        }
        // 计算实际要读取的范围（防止超出文件大小）
        long end = Math.min(offset + length - 1, objectMetadata.contentLength() - 1);
        // 创建获取对象的请求并设置范围
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(defaultBucketName)
                .key(objectName)
                .range("bytes=" + offset + "-" + end) // 2.x 版本中范围通过字符串设置
                .build();
        try (ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(getObjectRequest);) {
            int bytesRead = inputStream.readNBytes(data, doff, length);
            // 更新EOF状态
            eof.set(offset + bytesRead >= objectMetadata.contentLength());
            return bytesRead;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取objectargs 对象的属性信息
     *
     * @return
     */
    private HeadObjectResponse getObjectMetadata() {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(defaultBucketName)
                .key(objectName)
                .build();
        return s3Client.headObject(headObjectRequest);
    }

    public boolean isOpenAppend() {
        // 判断当前版本控制是否打开
        return (getAccessMask() & SftpConstants.ACE4_APPEND_DATA) != 0;
    }

    public void append(byte[] data, int doff, int length) {

        // 获取本地临时目录

        // 判断当前文件是否在对象存储中已经存在，并且本地目录不存在，如果存在就下载文件，追加再写入对象存储中

        // 如果对象存储不存在，就创建临时文件写入磁盘中
        SeekableByteChannel seekableByteChannel = null;
        try {
            seekableByteChannel = prepareChannel(localPath + objectName);
            seekableByteChannel.write(ByteBuffer.wrap(data, doff, length));
            fileIsUploaded = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                seekableByteChannel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void write(byte[] data, int doff, int length, long offset) {
        SeekableByteChannel seekableByteChannel = null;
        try {
            seekableByteChannel = prepareChannel(localPath + objectName);
            seekableByteChannel = seekableByteChannel.position(offset);
            seekableByteChannel.write(ByteBuffer.wrap(data, doff, length));
            fileIsUploaded = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                seekableByteChannel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void asyncPut() {
        // 异步上传示例
        CompletableFuture<PutObjectResponse> future = s3AsyncClient.putObject(
                PutObjectRequest.builder()
                        .bucket(defaultBucketName)
                        .key(objectName)
                        .build(),
                Paths.get(localPath + objectName)
        );

        // 处理异步结果
        future.whenComplete((response, exception) -> {
            if (exception == null) {
                logger.info("上传成功，ETag: {}", response.eTag());

            } else {
                logger.error("上传失败，ETag: {}", exception.getMessage());

            }
        });
    }


}
