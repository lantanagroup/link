package com.lantanagroup.link.cli;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class SftpDownloaderConfig {
  @NotEmpty
  private String knownHosts;

  @NotEmpty
  private String host;

  @Min(0)
  @Max(0xffff)
  private int port = 22;

  @NotEmpty
  private String path;

  @NotEmpty
  private String username;

  @NotEmpty
  private String password;

  private String fileName;
}
