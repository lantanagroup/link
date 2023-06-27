package com.lantanagroup.link.cli;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.lantanagroup.link.auth.OAuth2Helper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import java.text.SimpleDateFormat;
import java.util.Date;

@ShellComponent
public class ParklandInventoryImportCommand extends BaseShellCommand {
  private static final Logger logger = LoggerFactory.getLogger(ParklandInventoryImportCommand.class);

  private ParklandInventoryImportConfig config;
  private String fileType;

  @ShellMethod(
          key = "parkland-inventory-import",
          value = "Download an inventory via SFTP and submit it to Link.")
  public void execute(String fileType, @ShellOption(defaultValue="") String fileName) {
    try {
      this.fileType = fileType;
      registerBeans();
      config = applicationContext.getBean(ParklandInventoryImportConfig.class);
      validate(config);
      SetConfigFileName(fileName);
      byte[] data = download();
      submit(data);
    } catch (Exception ex) {
      logger.error("Failure with process, will not continue");
    }
  }

  private byte[] download() throws Exception {
    byte[] downloadedData;
    try {
      logger.info("Downloading {} from {}/{}",
              config.getDownloader().get(fileType).getFileName(),
              config.getDownloader().get(fileType).getHost(),
              config.getDownloader().get(fileType).getPath());
      SftpDownloader downloader = new SftpDownloader(config.getDownloader().get(fileType));
      downloadedData = downloader.download();
    } catch (Exception ex) {
      logger.error("Issue with download: {}", ex.getMessage());
      throw new Exception(ex);
    }

    return downloadedData;
  }

  private void submit(byte[] data) throws Exception {
    String submissionUrl = config.getSubmissionInfo().get(fileType).getSubmissionUrl();
    logger.info("Submitting to {}", submissionUrl);
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpPost request = new HttpPost(submissionUrl);
      AuthConfig auth = config.getSubmissionInfo().get(fileType).getSubmissionAuth();
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
      Utility.HttpPoster(request, logger);
    }
  }

  private void SetConfigFileName(String fileName) {
        /* The Parkland server path will have files named by day.  So...
      2023-06-04.xlsx
      2023-06-05.xlsx
      etc...

      One can run the command and specify a filename, but if that is blank, here we
      will default to the current date.
     */
    if (fileName == null || fileName.trim().isEmpty()){
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      String today = sdf.format(new Date());
      fileName = String.format("%s.%s", today, fileType);
    }

    config.getDownloader().get(fileType).setFileName(fileName);

  }
}
