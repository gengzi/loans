package com.gengzi.sftp.listener;

import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.FileHandle;
import org.apache.sshd.sftp.server.SftpEventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class FileWriteListener implements SftpEventListener {

    @Override
    public void writing(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen) throws IOException {

        String username = session.getUsername();

        System.out.printf(username + "写入" );

        SftpEventListener.super.writing(session, remoteHandle, localHandle, offset, data, dataOffset, dataLen);
    }
}
