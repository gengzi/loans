package com.gengzi;

import com.gengzi.sftp.config.MinIoClientConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MainTest {

    public static void main(String[] args) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://127.0.0.1:9000")
                .credentials("minioadmin", "minioadmin")
                .build();


        StatObjectResponse statObjectResponse = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket("image")
                        .object("/s3/file/approach.png")
                        .build());

        System.out.println(statObjectResponse.size());


        // get object data from offset to length of an SSE-C encrypted object
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("image")
                        .object("/s3/file/approach.png")
                        .offset(18459L)
                        .length(8000L)
                        .build())
        ) {
            // Read data from stream
            byte[] bytes1 = new byte[69999];
//           int i =  stream.read(bytes1, 0, bytes1.length);
//            System.out.println(i);
//            byte[] bytes = stream.readAllBytes();
//            String s = new String(bytes,"gbk");
//            System.out.println(s);

            int i1 = stream.readNBytes(bytes1, 18459, 8000);
            System.out.println(i1);


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
}
