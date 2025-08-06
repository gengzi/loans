package com.gengzi.sftp.handle;

import com.gengzi.sftp.config.MinIoClientConfig;
import com.gengzi.sftp.util.SpringContextUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.*;
import org.apache.sshd.sftp.server.Handle;
import org.apache.sshd.sftp.server.SftpSubsystem;
import org.simpleframework.xml.core.Complete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicReference;


/**
 * S3FileHandle
 */
public class S3FileHandle extends Handle {

    private MinioClient s3Client;

    private String defaultBucketName;

    private String objectName;

    public S3FileHandle(SftpSubsystem subsystem, Path file, String handle) {
        super(subsystem, file, handle);
        MinIoClientConfig config = SpringContextUtil.getBean(MinIoClientConfig.class);
        this.s3Client = (MinioClient) SpringContextUtil.getBean("s3Client");
        defaultBucketName = config.getDefaultBucketName();
        objectName = file.toString();
    }


    /**
     * 读取文件
     * @param data  数据存放
     * @param doff 数据偏移
     * @param length 长度
     * @param offset 偏移
     * @param eof 文件是否结束
     * @return
     */
    public int read(byte[] data, int doff, int length, long offset, AtomicReference<Boolean> eof) {
        StatObjectResponse statObjectResponse = ObjectArgs();
        if(statObjectResponse.size() <= offset){
            return -1;
        }

        try (InputStream stream = s3Client.getObject(
                GetObjectArgs.builder()
                        .bucket(defaultBucketName)
                        .object(objectName)
                        .offset(offset)
                        .length((long) length)
                        .build())) {
            // Read data from stream
                int l = stream.readNBytes(data, doff, length);
            return l;
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        }

    }

    private StatObjectResponse ObjectArgs()  {
        StatObjectResponse statObjectResponse = null;
        try {
            statObjectResponse = s3Client.statObject(
                    StatObjectArgs.builder()
                            .bucket(defaultBucketName)
                            .object(objectName)
                            .build());
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
        return statObjectResponse;
    }

}
