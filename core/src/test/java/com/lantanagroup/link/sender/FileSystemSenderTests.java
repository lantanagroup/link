package com.lantanagroup.link.sender;

import com.lantanagroup.link.config.sender.FileSystemSenderConfig;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;

public class FileSystemSenderTests {
  @Test
  public void getFilePathTest_NoConfigPath() {
    FileSystemSender sender = new FileSystemSender();
    FileSystemSenderConfig config = new FileSystemSenderConfig();
    config.setFormat(FileSystemSenderConfig.Formats.JSON);
    sender.setConfig(config);
    Assert.assertThrows(IllegalArgumentException.class, () -> { sender.getFilePath("submission"); });
  }

  @Test
  public void getFilePathTest_ConfigPath() {
    FileSystemSenderConfig config = new FileSystemSenderConfig();
    config.setPath("C:\\users\\test\\some-folder");
    FileSystemSender sender = new FileSystemSender();
    sender.setConfig(config);

    Path path = sender.getFilePath("submission");
    Assert.assertNotNull(path);
    Assert.assertTrue(path.toString().startsWith(config.getPath()));
  }

}
