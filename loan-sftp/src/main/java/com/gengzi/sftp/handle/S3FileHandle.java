package com.gengzi.sftp.handle;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.gengzi.sftp.config.AmazonS3Config;
import com.gengzi.sftp.config.MinIoClientConfig;
import com.gengzi.sftp.util.SpringContextUtil;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.*;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.server.Handle;
import org.apache.sshd.sftp.server.SftpSubsystem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.sshd.sftp.server.FileHandle.getOpenOptions;


/**
 * S3FileHandle
 */
public class S3FileHandle extends Handle {

    private final int access;
    private final Set<StandardOpenOption> openOptions;
    private AmazonS3 s3Client;
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
        this.s3Client = (AmazonS3) SpringContextUtil.getBean("AmazonS3Client");
        defaultBucketName = config.getDefaultBucketName();
        objectName = file.toString();
        localPath = config.getLocalPath();
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
        ObjectMetadata objectMetadata = ObjectArgs();
        //  判断文件大小小于或者等于 offset，说明文件已经读取完毕了。
        if (objectMetadata.getContentLength() <= offset) {
            return -1;
        }
        GetObjectRequest getObjectRequest = new GetObjectRequest(defaultBucketName, objectName);
        getObjectRequest.setRange(offset, offset + length - 1);
        S3Object s3Object = s3Client.getObject(getObjectRequest);
        try (InputStream inputStream = s3Object.getObjectContent()) {
            int l = inputStream.readNBytes(data, doff, length);
            return l;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取objectargs 对象的属性信息
     *
     * @return
     */
    private ObjectMetadata ObjectArgs() {
        return s3Client.getObjectMetadata(defaultBucketName, objectName);
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
        }finally {
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
        }finally {
            try {
                seekableByteChannel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * 准备文件通道
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


    public  PutObjectResult asyncPut(){
        PutObjectResult result = s3Client.putObject(new PutObjectRequest(defaultBucketName, objectName, new File(localPath + objectName)));
        return  result;
    }



}
