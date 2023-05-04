package com.lantanagroup.link.nhsn;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.lantanagroup.link.*;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.sender.AzureBlobStorageConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.naming.ConfigurationException;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class AzureBlobStorageSender extends GenericSender implements IReportSender, IAzureBlobStorageSender {

  @Autowired
  @Setter
  private AzureBlobStorageConfig absConfig;

  // **********************************
  // * Constants
  // **********************************
  protected static Logger logger = LoggerFactory.getLogger(AzureBlobStorageSender.class);

  public AzureBlobStorageSender() { }

  /**
   * Uploads a file to an Azure container using an InputStream
   *
   * @param filename - name of file to add to the Azure container
   * @param inputStream - stream containing file data
   */
  @Override
  public void upload(String filename, InputStream inputStream) throws IOException {
    try {
      BlobContainerAsyncClient client = getAbsClientConnection();
      if(client == null) {
        String errorMessage = "Failed to create blob async client.";
        logger.error("Error: Blob upload ({}) to container '{}' was unsuccessful\n", filename, this.absConfig.getAzureStorageContainerName());
        throw new IOException(errorMessage);
      }

      BlobAsyncClient blobAsyncClient = client.getBlobAsyncClient(filename);
      Flux<ByteBuffer> data = Flux.just(ByteBuffer.wrap(inputStream.readAllBytes()));
      ParallelTransferOptions parallelTransferOptions = new ParallelTransferOptions();
      blobAsyncClient.upload(data, parallelTransferOptions, true).block();
    } catch (Exception ex) {
      logger.error("Error: Blob upload ({}) to container '{}' was unsuccessful\n", filename, this.absConfig.getAzureStorageContainerName(), ex);
      String errorMessage = String.format("Failed to send measure report: {}", ex.getMessage());
      throw new IOException(errorMessage);
    }

    logger.info("Blob upload ({}) to container '{}' was successful.", filename, this.absConfig.getAzureStorageContainerName());
  }

  @Override
  public String sendContent(Resource resourceToSend, Report report) throws Exception {
    logger.info("Sending MeasureReport bundle to blob storage container {}.", this.absConfig.getAzureStorageContainerName());

    String bundleSerialization;
    if (absConfig.getFormat() == AzureBlobStorageSenderFormats.JSON) {
      bundleSerialization = FhirContextProvider.getFhirContext().newJsonParser().encodeResourceToString(resourceToSend);
    } else if (absConfig.getFormat() == AzureBlobStorageSenderFormats.XML) {
      bundleSerialization = FhirContextProvider.getFhirContext().newXmlParser().encodeResourceToString(resourceToSend);
    } else {
      logger.info("Missing format in abs configuration.");
      throw new ConfigurationException("Missing abs format configuration, needs to be json or xml.");
    }

    // set file name
    String fileName = report.getId() + "_" + new SimpleDateFormat("yyyyMMdd'T'HH_mm_ss").format(new Date());

    try (ByteArrayInputStream stream = new ByteArrayInputStream(bundleSerialization.getBytes(StandardCharsets.UTF_8))) {
      this.upload(fileName, stream);
      logger.info("Send to upload here");
    } catch (Exception ex) {
      logger.error("Failed to send measure report to blob storage: {}", Helper.encodeLogging(ex.getMessage()));
      throw new Exception(ex.getMessage());
    }

    return "MeasureReport sent successfully to blob storage";
  }

  @Override
  public void send(TenantService tenantService, Bundle submissionBundle, Report report, HttpServletRequest request, LinkCredentials user) throws Exception {
    this.sendContent(submissionBundle, report);
  }

  /**
   * Creates the blob service client using absConfig
   */
  private BlobContainerAsyncClient getAbsClientConnection() {
    try{
      BlobServiceAsyncClient svcClient = new BlobServiceClientBuilder()
              .endpoint(this.absConfig.getAddress())
              .sasToken(this.absConfig.getSecret())
              .buildAsyncClient();

      return svcClient.getBlobContainerAsyncClient(this.absConfig.getAzureStorageContainerName());
    }
    catch (Exception ex) {
      logger.error("Failed to send measure report to blob storage: {}", Helper.encodeLogging(ex.getMessage()));
    }

    return null;

  }

}
