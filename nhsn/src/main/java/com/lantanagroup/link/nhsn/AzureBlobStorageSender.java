package com.lantanagroup.link.nhsn;

import com.azure.core.util.BinaryData;
import com.lantanagroup.link.*;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.sender.AzureBlobStorageConfig;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import javax.naming.ConfigurationException;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
public class AzureBlobStorageSender extends GenericSender implements IReportSender, IAzureBlobStorageSender {

  @Autowired
  @Setter
  private AzureBlobStorageConfig absConfig;

  // **********************************
  // * Constants
  // **********************************
  protected static Logger logger = LoggerFactory.getLogger(AzureBlobStorageSender.class);

  // **********************************
  // * Members
  // **********************************
  private BlobContainerAsyncClient client;

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
      getAbsClientConnection();
      BlobAsyncClient blobAsyncClient = client.getBlobAsyncClient(filename);
      Flux<ByteBuffer> data = Flux.just(ByteBuffer.wrap(inputStream.readAllBytes()));
      ParallelTransferOptions parallelTransferOptions = new ParallelTransferOptions();
      blobAsyncClient.upload(data, parallelTransferOptions, true).block();
    } catch (Exception ex) {
      logger.error("Error: Blob upload ({}) to container '{}' was unsuccessful\n", filename, this.absConfig.getAzureStorageContainerName(), ex);
      String errorMessage = String.format("Failed to send measure report: %s", ex.getMessage());
      throw new IOException(errorMessage);
    }

    logger.info("Blob upload ({}) to container '{}' was successful.", filename, this.absConfig.getAzureStorageContainerName());
  }

  /**
   * Uploads a file to an Azure container using a file path.
   *
   * @param filename - name of file to add to the Azure container
   * @param filepath - local filepath to the file to be uploaded
   */
  @Override
  public void upload(String filename, String filepath) throws IOException {
    try {
      getAbsClientConnection();
      BlobAsyncClient blobAsyncClient = client.getBlobAsyncClient(filename);
      blobAsyncClient.uploadFromFile(filepath, true).block();
    } catch (Exception ex) {
      logger.error("Error: Blob upload ({}) to container '{}' was unsuccessful\n", filename, this.absConfig.getAzureStorageContainerName(), ex);
      String errorMessage = String.format("Failed to send measure report: %s", ex.getMessage());
      throw new IOException(errorMessage);
    }

    logger.info("Blob upload ({}) to container '{}' was successful.", filename, this.absConfig.getAzureStorageContainerName());
  }

  /**
   * Uploads a file to an Azure container using BinaryData
   *
   * @param filename - name of file to add to the Azure container
   * @param data - file data to be uploaded
   */
  @Override
  public void upload(String filename, BinaryData data) throws IOException {
    try {
      getAbsClientConnection();
      BlobAsyncClient blobAsyncClient = client.getBlobAsyncClient(filename);
      blobAsyncClient.upload(data, true).block();
    } catch (Exception ex) {
      logger.error("Error: Blob upload ({}) to container '{}' was unsuccessful\n", filename, this.absConfig.getAzureStorageContainerName(), ex);
      String errorMessage = String.format("Failed to send measure report: %s", ex.getMessage());
      throw new IOException(errorMessage);
    }

    logger.info("Blob upload ({}) to container '{}' was successful.", filename, this.absConfig.getAzureStorageContainerName());
  }

  @Override
  public String sendContent(Resource resourceToSend, DocumentReference documentReference, FhirDataProvider fhirStoreProvider) throws Exception {

    logger.info("Sending MeasureReport bundle to blob storage container %s.", this.absConfig.getAzureStorageContainerName());

    String bundleSerialization;
    if(absConfig.getFormat() == AzureBlobStorageSenderFormats.JSON) {
      bundleSerialization = fhirStoreProvider.bundleToJson((Bundle)resourceToSend);
    }
    else if(absConfig.getFormat() == AzureBlobStorageSenderFormats.XML) {
      bundleSerialization = fhirStoreProvider.bundleToXml((Bundle)resourceToSend);
    }
    else {
      logger.info("Missing format in abs configuration.");
      throw new ConfigurationException("Missing abs format configuration, needs to be json or xml.");
    }

    try(ByteArrayInputStream stream = new ByteArrayInputStream(bundleSerialization.getBytes(StandardCharsets.UTF_8))) {
      this.upload(((Bundle) resourceToSend).getIdentifier().getValue(), stream);
      logger.info("Send to upload here");
    }
    catch(Exception ex) {
      logger.error("Failed to send measure report to blob storage: %s", Helper.encodeLogging(ex.getMessage()));
      throw new Exception(ex.getMessage());
    }

//    try {
//      this.upload(((Bundle) resourceToSend).getIdentifier().getValue(), BinaryData.fromString(bundleSerialization));
//      logger.info("Send to upload here");
//    }
//    catch(Exception ex) {
//      logger.error("Failed to send measure report to blob storage: %s", Helper.encodeLogging(ex.getMessage()));
//      throw new Exception(ex.getMessage());
//    }

    return "MeasureReport sent successfully to blob storage";
  }

  @Override
  public String bundle(Bundle bundle, FhirDataProvider fhirProvider, String type) {
    return  null;
  }

  @Override
  public void send(MeasureReport masterMeasureReport, DocumentReference documentReference, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle, boolean removeGeneratedObservations) throws Exception {
    Bundle bundle = this.generateBundle(documentReference, masterMeasureReport, fhirDataProvider, sendWholeBundle, removeGeneratedObservations);
    this.sendContent(bundle, documentReference, fhirDataProvider);
    FhirHelper.recordAuditEvent(request, fhirDataProvider, ((LinkCredentials) auth.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully sent report");
  }

  /**
   * Creates the blob service client using absConfig
   */
  private void getAbsClientConnection() {
    try{
      BlobServiceAsyncClient svcClient = new BlobServiceClientBuilder()
              .endpoint(this.absConfig.getAddress())
              .sasToken(this.absConfig.getSecret())
              .buildAsyncClient();

      client = svcClient.getBlobContainerAsyncClient(this.absConfig.getAzureStorageContainerName());
    }
    catch (Exception ex) {
      logger.error("Failed to send measure report to blob storage: %s", Helper.encodeLogging(ex.getMessage()));
    }

  }

}
