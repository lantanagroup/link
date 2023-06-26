package com.lantanagroup.link.cli;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SftpDownloader {
  private final String username;
  private final String password;
  private final String host;
  private final int port;
  private final String knownHosts;
  private final String filePath;

  public SftpDownloader(SftpDownloaderConfig config) {
    username = config.getUsername();
    password = config.getPassword();
    host = config.getHost();
    port = config.getPort();
    knownHosts = config.getKnownHosts();
    filePath = SetPath(config.getPath(), config.getFileName());
  }

  public byte[] download() throws IOException, JSchException, SftpException {
    JSch jSch = new JSch();
    jSch.setKnownHosts(knownHosts);
    Session session = jSch.getSession(username, host, port);
    session.setPassword(password);
    session.connect();
    try {
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      try (InputStream stream = channel.get(filePath)) {
        return stream.readAllBytes();
      } finally {
        channel.exit();
      }
    } finally {
      session.disconnect();
    }
  }

  private String SetPath(String path, String fileName) {
    Path filePath = Paths.get(path);

    return String.valueOf(filePath.resolve(fileName));
  }
}
