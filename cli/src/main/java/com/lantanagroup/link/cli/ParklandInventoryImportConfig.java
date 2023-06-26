package com.lantanagroup.link.cli;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashMap;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cli.parkland-inventory-import")
public class ParklandInventoryImportConfig {

  @Getter
  private HashMap<String, SftpDownloaderConfig> downloader;
  @Getter
  private HashMap<String, ParklandSubmissionInfo> submissionInfo;

}
