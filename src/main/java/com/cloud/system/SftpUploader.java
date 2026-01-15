package com.cloud.system;

import com.jcraft.jsch.*;
import java.io.File;
import java.io.FileInputStream;

public class SftpUploader {

    public static void upload(String host, int port, String user, String password, File localFile) {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            // 1. Create a session
            session = jsch.getSession(user, host, port);
            session.setPassword(password);

            // Skip strict key checking for local development
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();
            System.out.println("🔗 SSH Session connected to " + host);

            // 2. Open the SFTP channel
            Channel channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            // 3. Transfer the file
            String remoteDir = "/home/storage_user/uploads/";
            channelSftp.put(new FileInputStream(localFile), remoteDir + localFile.getName());

            System.out.println("🚀 File transferred successfully to " + host);

        } catch (Exception e) {
            System.err.println("❌ SFTP Error on " + host);
            e.printStackTrace();
        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            if (session != null) session.disconnect();
        }
    }
}