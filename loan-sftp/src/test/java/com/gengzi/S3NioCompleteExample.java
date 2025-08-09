package com.gengzi;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.nio.spi.s3.S3FileSystem;
import software.amazon.nio.spi.s3.S3FileSystemProvider;
import software.amazon.nio.spi.s3.S3XFileSystemProvider;
import software.amazon.nio.spi.s3.config.S3NioSpiConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class S3NioCompleteExample {

    // S3 连接配置参数
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";
    private static final String BUCKET_NAME = "image";
    private static final Region REGION = Region.US_EAST_1;
    // 非 AWS 服务端点配置（如 MinIO），注释掉则使用 AWS 官方服务
     private static final String ENDPOINT_OVERRIDE = "localhost:9000";

    public static void main(String[] args) {
        // 创建文件系统并执行操作
        try (FileSystem s3FileSystem = createS3FileSystem()) {
            // 1. 上传本地文件到 S3
            uploadFile(s3FileSystem, "E:\\3.txt", "test.txt");
            
            // 2. 下载 S3 文件到本地
            downloadFile(s3FileSystem, "remote-folder/test.txt", "downloaded-test.txt");
            
            // 3. 列举 S3 目录下的文件
            listFiles(s3FileSystem, "remote-folder/");
            
            // 4. 读取 S3 文件内容
            readFileContent(s3FileSystem, "remote-folder/test.txt");
            
            // 5. 创建 S3 目录（虚拟）
            createDirectory(s3FileSystem, "new-folder/subfolder/");
            
            // 6. 删除 S3 文件
            deleteFile(s3FileSystem, "remote-folder/test.txt");
            
        } catch (IOException | URISyntaxException e) {
            System.err.println("操作 S3 时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建 S3 文件系统
     */
    private static FileSystem createS3FileSystem() throws IOException, URISyntaxException {
        // 配置环境变量
        Map<String, Object> env = new HashMap<>();
        env.put("aws.accessKeyId", ACCESS_KEY);
        env.put("aws.secretAccessKey", SECRET_KEY);
        env.put("aws.region", REGION.id());

        // 配置非 AWS 服务（如 MinIO）
         env.put("s3.endpointOverride", ENDPOINT_OVERRIDE);
         env.put("s3.pathStyleAccess", true); // 路径风格访问，MinIO 必需
//
//        // 创建并返回 S3 文件系统
        // s3x://[key:secret@]endpoint[:port]/bucket
        URI s3Uri = URI.create("s3://"+BUCKET_NAME);



        // 配置访问凭证
        AwsBasicCredentials credentials = AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);

        // 构建S3客户端
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(ENDPOINT_OVERRIDE)) // 设置MinIO服务端点
                .region(Region.of(REGION.id())) // 设置区域
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(config -> config
                        .pathStyleAccessEnabled(true) // 启用路径风格访问（MinIO推荐）
                )
                .build();

        S3AsyncClient build = S3AsyncClient.builder()
                .endpointOverride(URI.create(ENDPOINT_OVERRIDE)) // MinIO 服务地址
                .region(Region.of(REGION.id())) // MinIO 通常使用自定义区域或 us-east-1
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)
                ))
                .serviceConfiguration(service -> service
                        .pathStyleAccessEnabled(true) // MinIO 推荐启用路径风格
                )
                .build();
        S3FileSystemProvider s3FileSystemProvider = new S3FileSystemProvider();

        URI s3Urix = URI.create("s3://minioadmin:minioadmin@127.0.0.1:9000/image");
        S3XFileSystemProvider s3xFileSystemProvider = new S3XFileSystemProvider();
        FileSystem fileSystem = s3xFileSystemProvider.getFileSystem(s3Urix);
//        S3NioSpiConfiguration s3NioSpiConfiguration = new S3NioSpiConfiguration();
//        S3FileSystem s3FileSystem = new S3FileSystem(s3FileSystemProvider,s3NioSpiConfiguration);

//        env.put("s3.client",build);

        return fileSystem;
    }

    /**
     * 上传本地文件到 S3
     * @param s3Fs S3 文件系统
     * @param localFilePath 本地文件路径
     * @param s3Key S3 中的对象键（路径）
     */
    private static void uploadFile(FileSystem s3Fs, String localFilePath, String s3Key) throws IOException {
        Path localPath = Paths.get(localFilePath);
        Path s3Path = s3Fs.getPath(s3Key);
        
        // 检查本地文件是否存在
        if (!Files.exists(localPath)) {
            System.err.println("本地文件不存在: " + localFilePath);
            return;
        }
        
        // 执行上传
        Files.copy(localPath, s3Path, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("文件上传成功: " + s3Key);
    }

    /**
     * 从 S3 下载文件到本地
     * @param s3Fs S3 文件系统
     * @param s3Key S3 中的对象键
     * @param localFilePath 本地保存路径
     */
    private static void downloadFile(FileSystem s3Fs, String s3Key, String localFilePath) throws IOException {
        Path s3Path = s3Fs.getPath(s3Key);
        Path localPath = Paths.get(localFilePath);
        
        // 检查 S3 文件是否存在
        if (!Files.exists(s3Path)) {
            System.err.println("S3 文件不存在: " + s3Key);
            return;
        }
        
        // 执行下载
        Files.copy(s3Path, localPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("文件下载成功: " + localFilePath);
    }

    /**
     * 列举 S3 目录下的文件
     * @param s3Fs S3 文件系统
     * @param directoryPath S3 目录路径
     */
    private static void listFiles(FileSystem s3Fs, String directoryPath) throws IOException {
        Path dirPath = s3Fs.getPath(directoryPath);
        
        if (!Files.exists(dirPath)) {
            System.err.println("S3 目录不存在: " + directoryPath);
            return;
        }
        
        System.out.println("\n列举 " + directoryPath + " 下的文件:");
        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.forEach(path -> System.out.println("- " + path.getFileName()));
        }
    }

    /**
     * 读取 S3 文件内容
     * @param s3Fs S3 文件系统
     * @param s3Key S3 中的对象键
     */
    private static void readFileContent(FileSystem s3Fs, String s3Key) throws IOException {
        Path s3Path = s3Fs.getPath(s3Key);
        
        if (!Files.exists(s3Path)) {
            System.err.println("S3 文件不存在: " + s3Key);
            return;
        }
        
        // 读取文件内容（适用于文本文件）
        String content = Files.readString(s3Path);
        System.out.println("\n文件 " + s3Key + " 内容:");
        System.out.println(content);
    }

    /**
     * 创建 S3 目录（虚拟目录）
     * @param s3Fs S3 文件系统
     * @param directoryPath 要创建的目录路径
     */
    private static void createDirectory(FileSystem s3Fs, String directoryPath) throws IOException {
        Path dirPath = s3Fs.getPath(directoryPath);
        
        if (Files.exists(dirPath)) {
            System.out.println("目录已存在: " + directoryPath);
            return;
        }
        
        // 创建多级目录
        Files.createDirectories(dirPath);
        System.out.println("\n目录创建成功: " + directoryPath);
    }

    /**
     * 删除 S3 文件
     * @param s3Fs S3 文件系统
     * @param s3Key 要删除的对象键
     */
    private static void deleteFile(FileSystem s3Fs, String s3Key) throws IOException {
        Path s3Path = s3Fs.getPath(s3Key);
        
        if (!Files.exists(s3Path)) {
            System.err.println("S3 文件不存在: " + s3Key);
            return;
        }
        
        Files.delete(s3Path);
        System.out.println("\n文件删除成功: " + s3Key);
    }
}
