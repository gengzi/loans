package com.gengzi.sftp.handle;

import com.gengzi.sftp.stream.S3DirectoryStream;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.sftp.server.DirectoryHandle;
import org.apache.sshd.sftp.server.FileHandle;
import org.apache.sshd.sftp.server.SftpFileSystemAccessor;
import org.apache.sshd.sftp.server.SftpSubsystemProxy;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.Set;

public class MySftpFileSystemAccessor implements SftpFileSystemAccessor {

    /**
     * 打开目录,返回一个文件流
     * @param subsystem   The SFTP subsystem instance that manages the session
     * @param dirHandle   The {@link DirectoryHandle} representing the stream
     * @param dir         The requested <U>local</U> directory {@link Path} - same one returned by
     *                     {@link #resolveLocalFilePath(SftpSubsystemProxy, Path, String) resolveLocalFilePath}
     * @param handle      The assigned directory handle through which the remote peer references this directory
     * @param linkOptions
     * @return
     * @throws IOException
     */
    @Override
    public DirectoryStream<Path> openDirectory(SftpSubsystemProxy subsystem, DirectoryHandle dirHandle, Path dir, String handle, LinkOption... linkOptions) throws IOException {
        System.out.println(dir);
        // 根据dir 去查询数据库获取文件列表，返回文件列表流
        if(true){
            // 构造一个list
            ArrayList<Path> paths1 = new ArrayList<>();
            Path fileDir = Paths.get("data");
            Path file = Paths.get("1.txt");
            paths1.add(fileDir);
            paths1.add(file);
            S3DirectoryStream paths = new S3DirectoryStream(paths1);
            return paths;
        }else{
            return SftpFileSystemAccessor.super.openDirectory(subsystem, dirHandle, dir, handle, linkOptions);
        }

    }

    @Override
    public LinkOption[] resolveFileAccessLinkOptions(SftpSubsystemProxy subsystem, Path file, int cmd, String extension, boolean followLinks) throws IOException {
        // 禁止软链接 符号链接
        return SftpFileSystemAccessor.super.resolveFileAccessLinkOptions(subsystem, file, cmd, extension, followLinks);
    }

    @Override
    public void putRemoteFileName(SftpSubsystemProxy subsystem, Path path, Buffer buf, String name, boolean shortName) throws IOException {
        // 设置远程文件名称
        SftpFileSystemAccessor.super.putRemoteFileName(subsystem, path, buf, name, shortName);
    }

    /**
     *  解析报告文件属性
     * @param subsystem   The SFTP subsystem instance that manages the session
     * @param file        The referenced file
     * @param flags       A mask of the original required attributes
     * @param attrs       The default resolved attributes map
     * @param options     The {@link LinkOption}-s that were used to access the file's attributes
     * @return
     * @throws IOException
     */
    @Override
    public NavigableMap<String, Object> resolveReportedFileAttributes(SftpSubsystemProxy subsystem, Path file, int flags, NavigableMap<String, Object> attrs, LinkOption... options) throws IOException {
        return SftpFileSystemAccessor.super.resolveReportedFileAttributes(subsystem, file, flags, attrs, options);
    }

    @Override
    public SeekableByteChannel openFile(SftpSubsystemProxy subsystem, FileHandle fileHandle, Path file, String handle, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return SftpFileSystemAccessor.super.openFile(subsystem, fileHandle, file, handle, options, attrs);
    }
}
