package com.lantanagroup.link.cli;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;

public class SftpDownloader {
  private final SftpDownloaderConfig config;

  public SftpDownloader(SftpDownloaderConfig config) {
    this.config = config;
  }

  public byte[] download() throws IOException, JSchException, SftpException {
    JSch jSch = new JSch();
    jSch.setKnownHosts(config.getKnownHosts());
    Session session = jSch.getSession(config.getUsername(), config.getHost(), config.getPort());
    session.setPassword(config.getPassword());
    session.connect();
    ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
    channel.connect();
    try (InputStream stream = channel.get(config.getPath())) {
      return stream.readAllBytes();
    }
  }
}
