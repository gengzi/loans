package com.gengzi.sfpt.config;


import com.gengzi.sfpt.listener.FileWriteListener;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;


/**
 * 配置sftp相关配置项
 *
 */
@Configuration
public class SfrpServerConfig {

    @Value("${sftp.server.port}")
    private int sftpPort;

//    @Value("${sftp.server.host-key-path}")
//    private String hostKeyPath;
//
//    @Value("${sftp.server.root-directory}")
//    private String rootDirectory;

    @Autowired
    private FileWriteListener fileWriteListener;

    @Bean
    public SshServer sftpServer() throws IOException {
        SshServer server = SshServer.setUpDefaultServer();
        server.setHost("0.0.0.0");
        server.setPort(sftpPort);
        // 配置主机密钥（首次启动会自动生成）

        server.setKeyPairProvider(new FileKeyPairProvider(Paths.get(new File("D:\\work\\loans\\loan-sfpt\\hostkey.ser").getAbsolutePath())));

        SftpSubsystemFactory factory = new SftpSubsystemFactory();
        server.setSubsystemFactories(Collections.singletonList(factory));

        factory.addSftpEventListener(new SftpEventListener() {
            @Override
            public void received(ServerSession session, int type, int id) throws IOException {
                SftpEventListener.super.received(session, type, id);
            }

            @Override
            public void receivedExtension(ServerSession session, String extension, int id) throws IOException {
                SftpEventListener.super.receivedExtension(session, extension, id);
            }

            @Override
            public void initialized(ServerSession session, int version) throws IOException {
                SftpEventListener.super.initialized(session, version);
            }

            @Override
            public void exiting(ServerSession session, Handle handle) throws IOException {
                SftpEventListener.super.exiting(session, handle);
            }

            @Override
            public void destroying(ServerSession session) throws IOException {
                SftpEventListener.super.destroying(session);
            }

            @Override
            public void opening(ServerSession session, String remoteHandle, Handle localHandle) throws IOException {
                SftpEventListener.super.opening(session, remoteHandle, localHandle);
            }

            @Override
            public void open(ServerSession session, String remoteHandle, Handle localHandle) throws IOException {
                SftpEventListener.super.open(session, remoteHandle, localHandle);
            }

            @Override
            public void openFailed(ServerSession session, String remotePath, Path localPath, boolean isDirectory, Throwable thrown) throws IOException {
                SftpEventListener.super.openFailed(session, remotePath, localPath, isDirectory, thrown);
            }

            @Override
            public void readingEntries(ServerSession session, String remoteHandle, DirectoryHandle localHandle) throws IOException {
                SftpEventListener.super.readingEntries(session, remoteHandle, localHandle);
            }

            @Override
            public void readEntries(ServerSession session, String remoteHandle, DirectoryHandle localHandle, Map<String, Path> entries) throws IOException {
                SftpEventListener.super.readEntries(session, remoteHandle, localHandle, entries);
            }

            @Override
            public void reading(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen) throws IOException {
                SftpEventListener.super.reading(session, remoteHandle, localHandle, offset, data, dataOffset, dataLen);
            }

            @Override
            public void read(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen, int readLen, Throwable thrown) throws IOException {
                SftpEventListener.super.read(session, remoteHandle, localHandle, offset, data, dataOffset, dataLen, readLen, thrown);
            }

            @Override
            public void writing(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen) throws IOException {
                fileWriteListener.writing(session,remoteHandle,localHandle,offset,data,dataOffset,dataLen);
            }

            @Override
            public void written(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen, Throwable thrown) throws IOException {
                SftpEventListener.super.written(session, remoteHandle, localHandle, offset, data, dataOffset, dataLen, thrown);
            }

            @Override
            public void blocking(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, int mask) throws IOException {
                SftpEventListener.super.blocking(session, remoteHandle, localHandle, offset, length, mask);
            }

            @Override
            public void blocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, int mask, Throwable thrown) throws IOException {
                SftpEventListener.super.blocked(session, remoteHandle, localHandle, offset, length, mask, thrown);
            }

            @Override
            public void unblocking(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length) throws IOException {
                SftpEventListener.super.unblocking(session, remoteHandle, localHandle, offset, length);
            }

            @Override
            public void unblocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, Throwable thrown) throws IOException {
                SftpEventListener.super.unblocked(session, remoteHandle, localHandle, offset, length, thrown);
            }

            @Override
            public void closing(ServerSession session, String remoteHandle, Handle localHandle) throws IOException {
                SftpEventListener.super.closing(session, remoteHandle, localHandle);
            }

            @Override
            public void closed(ServerSession session, String remoteHandle, Handle localHandle, Throwable thrown) throws IOException {
                SftpEventListener.super.closed(session, remoteHandle, localHandle, thrown);
            }

            @Override
            public void creating(ServerSession session, Path path, Map<String, ?> attrs) throws IOException {
                SftpEventListener.super.creating(session, path, attrs);
            }

            @Override
            public void created(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) throws IOException {
                SftpEventListener.super.created(session, path, attrs, thrown);
            }

            @Override
            public void moving(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts) throws IOException {
                SftpEventListener.super.moving(session, srcPath, dstPath, opts);
            }

            @Override
            public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts, Throwable thrown) throws IOException {
                SftpEventListener.super.moved(session, srcPath, dstPath, opts, thrown);
            }

            @Override
            public void removing(ServerSession session, Path path, boolean isDirectory) throws IOException {
                SftpEventListener.super.removing(session, path, isDirectory);
            }

            @Override
            public void removed(ServerSession session, Path path, boolean isDirectory, Throwable thrown) throws IOException {
                SftpEventListener.super.removed(session, path, isDirectory, thrown);
            }

            @Override
            public void linking(ServerSession session, Path source, Path target, boolean symLink) throws IOException {
                SftpEventListener.super.linking(session, source, target, symLink);
            }

            @Override
            public void linked(ServerSession session, Path source, Path target, boolean symLink, Throwable thrown) throws IOException {
                SftpEventListener.super.linked(session, source, target, symLink, thrown);
            }

            @Override
            public void modifyingAttributes(ServerSession session, Path path, Map<String, ?> attrs) throws IOException {
                SftpEventListener.super.modifyingAttributes(session, path, attrs);
            }

            @Override
            public void modifiedAttributes(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) throws IOException {
                SftpEventListener.super.modifiedAttributes(session, path, attrs, thrown);
            }
        });

        // 配置密码认证器
        server.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session)
                    throws PasswordChangeRequiredException {
                // 这里实现自定义的用户名密码验证逻辑
                // 实际应用中应从数据库或安全存储中验证
                // 示例：允许用户"admin"使用密码"admin123"登录
                return "admin".equals(username) && "admin123".equals(password);
            }
        });
        // 设置文件系统根目录
        server.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get("D:\\work\\loans\\loan-sfpt")));

        server.start();
        return server;
    }

    private void createDirectoryIfNotExists(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }


}

