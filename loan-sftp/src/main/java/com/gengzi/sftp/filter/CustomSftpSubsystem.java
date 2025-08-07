package com.gengzi.sftp.filter;

import com.gengzi.sftp.handle.S3DoStat;
import com.gengzi.sftp.handle.S3FileHandle;
import org.apache.sshd.common.util.ValidateUtils;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.BufferUtils;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.SftpModuleProperties;
import org.apache.sshd.sftp.client.impl.SftpPathImpl;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.apache.sshd.sftp.common.SftpHelper;
import org.apache.sshd.sftp.server.*;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自定义SFTP文件系统视图，修改文件读取逻辑
 */
public class CustomSftpSubsystem extends SftpSubsystem {


    // 存储s3文件句柄对应的处理类
    protected final Map<String, Handle> s3FileHandles = new ConcurrentHashMap<>();

    /**
     * @param channel      The {@link ChannelSession} through which the command was received
     * @param configurator The {@link SftpSubsystemConfigurator} to use
     */
    public CustomSftpSubsystem(ChannelSession channel, SftpSubsystemConfigurator configurator) {
        super(channel, configurator);
    }

    @Override
    protected void process(Buffer buffer) throws IOException {
        super.process(buffer);
    }

    @Override
    protected void doProcess(Buffer buffer, int length, int type, int id) throws IOException {
        String statusName = SftpConstants.getCommandMessageName(type);
        log.info("doProcess :type={} typeName={} [id={}][length={}][buffer={}] ", type, statusName, id, length, buffer);
        super.doProcess(buffer, length, type, id);
    }


    @Override
    protected Map<String, Object> doLStat(int id, String path, int flags) throws IOException {
        // 根据路径获取对应目录下的 文件
        // 获取根据路径+文件名称 获取文件句柄
        log.info("doLStat :path={} [id={}][flags={}] ", path, id, flags);
        if (path != null && path.startsWith("/s3")) {
            return S3DoStat.doStat(id, path, flags);
        }
        Map<String, Object> stringObjectMap = super.doLStat(id, path, flags);
        for (Map.Entry<String, Object> stringObjectEntry : stringObjectMap.entrySet()) {
            System.out.println(stringObjectEntry.getKey() + "," + stringObjectEntry.getValue());
        }
        return stringObjectMap;
    }


    @Override
    protected Map<String, Object> doStat(int id, String path, int flags) throws IOException {
        System.out.println(id + "," + path + "," + flags);
        if (path != null && path.startsWith("/s3")) {
            return S3DoStat.doStat(id, path, flags);
        }
        return super.doStat(id, path, flags);
    }

    @Override
    protected String doOpenDir(int id, String path, Path dir, LinkOption... options) throws IOException {
        return super.doOpenDir(id, path, dir, options);
    }

    @Override
    protected void doLStat(Buffer buffer, int id) throws IOException {
        super.doLStat(buffer, id);
    }

    @Override
    protected String doOpen(int id, String path, int pflags, int access, Map<String, Object> attrs) throws IOException {
        // 打开文件，并返回文件句柄
        // 当前磁盘不存在该文件，从对象存储中获取文件
        if (path.toString().startsWith("/s3")) {
            ServerSession session = getServerSession();
            if (log.isInfoEnabled()) {
                log.info("doOpen({})[id={}] SSH_FXP_OPEN (path={}, access=0x{}, pflags=0x{}, attrs={})",
                        session, id, path, Integer.toHexString(access), Integer.toHexString(pflags), attrs);
            }

            Path file = resolveFile(path);
            int curHandleCount = handles.size();
            int maxHandleCount = SftpModuleProperties.MAX_OPEN_HANDLES_PER_SESSION.getRequired(session);
            if (curHandleCount > maxHandleCount) {
                throw signalOpenFailure(id, path, file, false,
                        new SftpException(SftpConstants.SSH_FX_NO_SPACE_ON_FILESYSTEM,
                                "Too many open handles: current=" + curHandleCount + ", max.=" + maxHandleCount));
            }

            String handle;
            try {
                synchronized (s3FileHandles) {
                    handle = generateFileHandle(file);
                    S3FileHandle fileHandle = new S3FileHandle(this, file, handle);
                    s3FileHandles.put(handle, fileHandle);
                }
            } catch (IOException e) {
                throw signalOpenFailure(id, path, file, false, e);
            }

            return handle;
        }


        return super.doOpen(id, path, pflags, access, attrs);
    }

    @Override
    protected int doRead(int id, String handle, long offset, int length, byte[] data, int doff, AtomicReference<Boolean> eof) throws IOException {
        System.out.println(handle);
        S3FileHandle s3FileHandle = (S3FileHandle) s3FileHandles.get(handle);
        if (s3FileHandle != null) {

            ServerSession session = getServerSession();
            if (log.isInfoEnabled()) {
                log.info("doRead({})[id={}] SSH_FXP_READ (handle={}[{}], offset={}, length={})",
                        session, id, handle, s3FileHandle, offset, length);
            }
            // 校验文件长度
            ValidateUtils.checkTrue(length > 0L, "Invalid read length: %d", length);

            SftpEventListener listener = getSftpEventListenerProxy();
            int readLen;

            try {
                readLen = s3FileHandle.read(data, doff, length, offset, eof);
            } catch (RuntimeException | Error e) {
                throw e;
            }
            return readLen;

        }


        return super.doRead(id, handle, offset, length, data, doff, eof);
    }

    @Override
    protected void doReadDir(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        Handle h = handles.get(handle);
        ServerSession session = getServerSession();
        boolean debugEnabled = log.isDebugEnabled();
        if (debugEnabled) {
            log.debug("doReadDir({})[id={}] SSH_FXP_READDIR (handle={}[{}])", session, id, handle, h);
        }

        Buffer reply = null;
        try {
            DirectoryHandle dh = validateHandle(handle, h, DirectoryHandle.class);
            if (dh.isDone()) {
                sendStatus(prepareReply(buffer), id, SftpConstants.SSH_FX_EOF, "Directory reading is done");
                return;
            }

            Path file = dh.getFile();
            // If it's an SftpPath, don't re-check accessibily or existence. The underlying DirectoryHandle iterator
            // contacts the upstream server, which should check. This repeated check here is questionable anyway.
            // We did check in doOpenDir(). If access to the directory has changed in the meantime; it's undefined
            // anyway what happens. If the directory is local, it depends on what the Java library would do with
            // the DirectoryStream in that case if it reads the directory lazily. And if it is remote, it may well
            // be that the upstream server has read the whole list from the local file system and buffered it, so
            // it could serve the listing even if some other concurrent operation had removed the directory in the
            // meantime, or had changed its access. It is entirely unspecified what shall happen if files inside
            // the directory are changed while the directory is listed, and likewise it's entirely unspecified what
            // shall happen if the directory itself is deleted while being listed.
            //
            // As long as the file system is local, this check here is local operations only, but if the directory
            // is remote; this incurs several (up to three) remote LSTAT calls. We really can skip this here and let
            // the upstream server decide.
//            if (!(file instanceof SftpPath)) {
//                LinkOption[] options = getPathResolutionLinkOption(SftpConstants.SSH_FXP_READDIR, "", file);
//                Boolean status = IoUtils.checkFileExistsAnySymlinks(file, !IoUtils.followLinks(options));
//                if (status == null) {
//                    throw new AccessDeniedException(file.toString(), file.toString(), "Cannot determine existence of read-dir");
//                }
//
//                if (!status) {
//                    throw new NoSuchFileException(file.toString(), file.toString(), "Non-existent directory");
//                } else if (!Files.isDirectory(file, options)) {
//                    throw new NotDirectoryException(file.toString());
//                } else if (!Files.isReadable(file)) {
//                    throw new AccessDeniedException(file.toString(), file.toString(), "Not readable");
//                }
//            }

            SftpEventListener listener = getSftpEventListenerProxy();
            listener.readingEntries(session, handle, dh);

            if (dh.isSendDot() || dh.isSendDotDot() || dh.hasNext()) {
                // There is at least one file in the directory or we need to send the "..".
                // Send only a few files at a time to not create packets of a too
                // large size or have a timeout to occur.

                reply = prepareReply(buffer);
                reply.putByte((byte) SftpConstants.SSH_FXP_NAME);
                reply.putInt(id);

                int lenPos = reply.wpos();
                reply.putUInt(0L);  // save room for actual length

                int maxDataSize = SftpModuleProperties.MAX_READDIR_DATA_SIZE.getRequired(session);
                int count = doReadDir(id, handle, dh, reply, maxDataSize, false);
                BufferUtils.updateLengthPlaceholder(reply, lenPos, count);
                if ((!dh.isSendDot()) && (!dh.isSendDotDot()) && (!dh.hasNext())) {
                    dh.markDone();
                }

                int sftpVersion = getVersion();
                Boolean indicator = SftpHelper.indicateEndOfNamesList(reply, sftpVersion, session, dh.isDone());
                if (debugEnabled) {
                    log.debug("doReadDir({})({})[{}] - sending {} entries - eol={} (SFTP version {})", session, handle, h,
                            count, indicator, sftpVersion);
                }
            } else {
                // empty directory
                dh.markDone();
                sendStatus(prepareReply(buffer), id, SftpConstants.SSH_FX_EOF, "Empty directory");
                return;
            }

            Objects.requireNonNull(reply, "No reply buffer created");
        } catch (IOException | RuntimeException | Error e) {
            sendStatus(prepareReply(buffer), id, e, SftpConstants.SSH_FXP_READDIR, handle);
            return;
        }

        send(reply);


        super.doReadDir(buffer, id);
    }

    @Override
    protected int doReadDir(int id, String handle, DirectoryHandle dir, Buffer buffer, int maxSize, boolean followLinks) throws IOException {
        return super.doReadDir(id, handle, dir, buffer, maxSize, followLinks);
    }

    @Override
    protected NavigableMap<String, Object> resolveFileAttributes(Path path, int flags, boolean neverFollowSymLinks, LinkOption... options) throws IOException {
        return SftpPathImpl.withAttributeCache(path, file -> {
            return getAttributes(file, flags, options);
        });
    }


    @Override
    protected void doClose(int id, String handle) throws IOException {
        if (s3FileHandles.containsKey(handle)) {
            Handle nodeHandle = s3FileHandles.remove(handle);
            ServerSession session = getServerSession();
            SftpEventListener listener = getSftpEventListenerProxy();
            try {
                listener.closing(session, handle, nodeHandle);
                nodeHandle.close();
                listener.closed(session, handle, nodeHandle, null);
            } catch (IOException | RuntimeException | Error e) {
                listener.closed(session, handle, nodeHandle, e);
                throw e;
            } finally {
                nodeHandle.clearAttributes();
            }
            return;
        }
        super.doClose(id, handle);
    }


    @Override
    protected void doWrite(int id, String handle, long offset, int length, byte[] data, int doff, int remaining) throws IOException {

        S3FileHandle s3FileHandle = (S3FileHandle) s3FileHandles.get(handle);
        ServerSession session = getServerSession();
        int maxAllowed = SftpModuleProperties.MAX_WRITEDATA_PACKET_LENGTH.getRequired(session);
        if (log.isTraceEnabled()) {
            log.trace("doWrite({})[id={}] SSH_FXP_WRITE (handle={}[{}], offset={}, length={}, maxAllowed={})",
                    session, id, handle, s3FileHandle, offset, length, maxAllowed);
        }

        if (length < 0) {
            throw new IllegalStateException("Bad length (" + length + ") for writing to " + s3FileHandle);
        }

        if (remaining < length) {
            throw new IllegalStateException("Not enough buffer data for writing to " + s3FileHandle
                    + ": required=" + length + ", available=" + remaining);
        }

        if (length > maxAllowed) {
            throw new IOException("Reuested write size (" + length + ") exceeds max. allowed (" + maxAllowed + ")");
        }
        try {
            if (s3FileHandle.isOpenAppend()) {
                s3FileHandle.append(data, doff, length);
            } else {
                s3FileHandle.write(data, doff, length, offset);
            }
        } catch (RuntimeException | Error e) {
            throw e;
        }



         super.doWrite(id, handle, offset, length, data, doff, remaining);
    }
}
    