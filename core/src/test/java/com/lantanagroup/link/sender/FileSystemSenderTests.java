package com.lantanagroup.link.sender;

import com.lantanagroup.link.TestHelper;
import com.lantanagroup.link.config.sender.FileSystemSenderConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;

public class FileSystemSenderTests {
  @Test
  public void getFilePathTest_NoConfigPath() {
    FileSystemSender sender = new FileSystemSender();
    Assert.assertThrows(IllegalArgumentException.class, () -> { sender.getFilePath(); });
  }

  @Test
  public void getFilePathTest_ConfigPath() {
    FileSystemSenderConfig config = new FileSystemSenderConfig();
    config.setPath("C:\\users\\test\\some-folder");
    FileSystemSender sender = new FileSystemSender();
    sender.setConfig(config);

    Path path = sender.getFilePath();
    Assert.assertNotNull(path);
    Assert.assertTrue(path.toString().startsWith(config.getPath()));
  }

  @Test
  @Ignore
  public void testSendToFolder() throws Exception {
    FileSystemSenderConfig config = new FileSystemSenderConfig();
    config.setPath("%TEMP%");
    config.setIsBundle(false);

    Bundle bundle = TestHelper.getBundle("large-submission-example.json");
    FileSystemSender sender = new FileSystemSender();
    sender.setConfig(config);

    sender.send(null, bundle, null, null, null);
  }
}
