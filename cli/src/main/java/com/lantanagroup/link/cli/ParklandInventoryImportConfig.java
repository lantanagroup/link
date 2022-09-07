package com.lantanagroup.link.cli;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cli.parkland-inventory-import")
public class ParklandInventoryImportConfig {
  @NotNull
  private SftpDownloaderConfig downloader;

  @NotEmpty
  private String submissionUrl;

  private AuthConfig submissionAuth;
}
