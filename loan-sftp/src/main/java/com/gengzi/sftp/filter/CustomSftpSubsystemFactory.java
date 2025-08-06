package com.gengzi.sftp.filter;

import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.sftp.server.SftpSubsystem;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.Map;

/**
 * 自定义工厂类，用于创建CustomSftpSubsystem实例
 */
public class CustomSftpSubsystemFactory extends SftpSubsystemFactory {

    @Override
    public Command createSubsystem(ChannelSession channel) throws IOException {
        CustomSftpSubsystem subsystem = new CustomSftpSubsystem(channel, this);
        GenericUtils.forEach(getRegisteredListeners(), subsystem::addSftpEventListener);
        return subsystem;
    }

}
    