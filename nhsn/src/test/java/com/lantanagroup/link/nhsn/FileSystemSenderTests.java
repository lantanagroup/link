package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.config.sender.FileSystemSenderConfig;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;

public class FileSystemSenderTests {
    @Test
    public void getFilePathTest_NoConfigPath() {
        FileSystemSender sender = new FileSystemSender();
        Path path = sender.getFilePath();
        Assert.assertNotNull(path);
        Assert.assertNotEquals(0, path.toString().length());
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
}
