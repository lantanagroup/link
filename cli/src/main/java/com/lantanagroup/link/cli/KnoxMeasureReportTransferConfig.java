package com.lantanagroup.link.cli;

import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cli.knox-measure-report-transfer")
public class KnoxMeasureReportTransferConfig {
  @NotNull
  private SftpDownloaderConfig downloader;

  @NotEmpty
  private String measureUrl;

  @NotEmpty
  private String subjectIdentifier;

  @NotEmpty
  private String groupCode;

  @NotNull
  private FHIRSenderConfig fhirSender;
}
