package com.lantanagroup.link.cli;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.lantanagroup.link.auth.OAuth2Helper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class ParklandInventoryImportCommand extends BaseShellCommand {
  private static final Logger logger = LoggerFactory.getLogger(ParklandInventoryImportCommand.class);

  private ParklandInventoryImportConfig config;

  @ShellMethod(
          key = "parkland-inventory-import",
          value = "Download an inventory via SFTP and submit it to Link.")
  public void execute() throws Exception {
    registerBeans();
    config = applicationContext.getBean(ParklandInventoryImportConfig.class);
    validate(config);
    byte[] data = download();
    submit(data);
  }

  private byte[] download() throws Exception {
    logger.info("Downloading from {}/{}", config.getDownloader().getHost(), config.getDownloader().getPath());
    SftpDownloader downloader = new SftpDownloader(config.getDownloader());
    return downloader.download();
  }

  private void submit(byte[] data) throws Exception {
    logger.info("Submitting to {}", config.getSubmissionUrl());
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpPost request = new HttpPost(config.getSubmissionUrl());
      AuthConfig auth = config.getSubmissionAuth();
      if (auth != null) {
        String token = OAuth2Helper.getPasswordCredentialsToken(
                httpClient,
                auth.getTokenUrl(),
                auth.getUser(),
                auth.getPass(),
                auth.getClientId(),
                auth.getScope());
        if (token == null) {
          throw new Exception("Authorization failed");
        }
        if (OAuth2Helper.validateHeaderJwtToken(token)) {
          request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
        } else {
          throw new JWTVerificationException("Invalid token format");
        }
      }
      request.setEntity(new ByteArrayEntity(data));
      httpClient.execute(request, response -> {
        logger.info("Response: {}", response.getStatusLine());
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          String body = EntityUtils.toString(entity);
          if (StringUtils.isNotEmpty(body)) {
            logger.debug(body);
          }
        }
        return null;
      });
    }
  }
}
