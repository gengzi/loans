package com.gengzi.sftp.handle;

import com.gengzi.sftp.process.S3Do;
import com.gengzi.sftp.stream.S3ToDirectoryStream;
import org.apache.sshd.common.util.ValidateUtils;
import org.apache.sshd.sftp.server.DirectoryHandle;
import org.apache.sshd.sftp.server.Handle;
import org.apache.sshd.sftp.server.SftpFileSystemAccessor;
import org.apache.sshd.sftp.server.SftpSubsystem;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class S3DirectoryHandle extends Handle implements Iterator<Path>{

    // 是否读取结束
    private boolean done;
    // 发送父目录（..）
    private boolean sendDotDot = true;
    // 发送当前目录（.）
    private boolean sendDot = true;
    // the directory should be read once at "open directory"
    private DirectoryStream<Path> ds;
    private Iterator<Path> fileList;


    public S3DirectoryHandle(SftpSubsystem subsystem, Path file, String handle) throws IOException {
        super(subsystem, file, handle);
        S3Do s3Do = new S3Do();
        List<S3Object> s3Objects = s3Do.listFilesInDirectory(s3Do.getAmazonS3Config().getDefaultBucketName(), file.toString());
        this.ds = new S3ToDirectoryStream(s3Objects,file);
        this.fileList = ds.iterator();
        signalHandleOpening();

        try {
            signalHandleOpen();
        } catch (IOException e) {
            close();
            throw e;
        }
    }


    public boolean isDone() {
        return done;
    }

    public boolean isSendDot() {
        return sendDot;
    }


    public void markDone() {
        this.done = true;
        // allow the garbage collector to do the job
        this.fileList = null;
    }

    @Override
    public boolean hasNext() {
        return fileList.hasNext();
    }

    @Override
    public Path next() {
        return fileList.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not allowed to remove " + toString());
    }



    @Override
    public void close() throws IOException {
        try {

        } finally {
            markDone(); // just making sure
        }

    }

    public boolean isSendDotDot() {
        return sendDotDot;
    }

    public void markDotSent() {
        sendDot = false;
    }

    public void markDotDotSent() {
        sendDotDot = false;
    }
}
