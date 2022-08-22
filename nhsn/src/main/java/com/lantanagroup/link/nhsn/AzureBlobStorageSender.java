package com.lantanagroup.link.nhsn;

import com.azure.core.util.BinaryData;
import com.lantanagroup.link.IAzureBlobStorageSender;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.ParallelTransferOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class AzureBlobStorageSender implements IAzureBlobStorageSender {
  // **********************************
  // * Constants
  // **********************************
  protected static final Logger logger = LoggerFactory.getLogger(AzureBlobStorageSender.class);

  // **********************************
  // * Members
  // **********************************
  private String endpoint;
  private String sasToken;
  private String containerName;
  private final BlobContainerAsyncClient client;

  public AzureBlobStorageSender(String azureStorageConnectionString, String azureSasToken, String azureStorageContainerName) {
    this.endpoint = azureStorageConnectionString;
    this.sasToken = azureSasToken;
    this.containerName = azureStorageContainerName;

    BlobServiceAsyncClient svcClient = new BlobServiceClientBuilder()
            .endpoint(azureStorageConnectionString)
            .sasToken(azureSasToken)
            .buildAsyncClient();

    client = svcClient.getBlobContainerAsyncClient(azureStorageContainerName);
  }

  /**
   * Uploads a file to an Azure container using an InputStream
   *
   * @param filename - name of file to add to the Azure container
   * @param inputStream - stream containing file data
   */
  @Override
  public void upload(String filename, InputStream inputStream) {
    try {
      BlobAsyncClient blobAsyncClient = client.getBlobAsyncClient(filename);
      Flux<ByteBuffer> data = Flux.just(ByteBuffer.wrap(inputStream.readAllBytes()));
      ParallelTransferOptions parallelTransferOptions = new ParallelTransferOptions();
      blobAsyncClient.upload(data, parallelTransferOptions, true).block();
    } catch (Exception e) {
      logger.error("Error: Blob upload ({}) to container '{}' was unsuccessful\n", filename, this.containerName, e);
    }

    logger.info("Blob upload ({}) to container '{}' was successful.", filename, this.containerName);
  }

  /**
   * Uploads a file to an Azure container using a file path.
   *
   * @param filename - name of file to add to the Azure container
   * @param filepath - local filepath to the file to be uploaded
   */
  @Override
  public void upload(String filename, String filepath) {
    try {
      BlobAsyncClient blobAsyncClient = client.getBlobAsyncClient(filename);
      blobAsyncClient.uploadFromFile(filepath, true).block();
    } catch (Exception e) {
      logger.error("Error: Blob upload ({}) to container '{}' was unsuccessful\n", filename, this.containerName, e);
    }

    logger.info("Blob upload ({}) to container '{}' was successful.", filename, this.containerName);
  }

  /**
   * Uploads a file to an Azure container using BinaryData
   *
   * @param filename - name of file to add to the Azure container
   * @param data - file data to be uploaded
   */
  @Override
  public void upload(String filename, BinaryData data) {
    try {
      BlobAsyncClient blobAsyncClient = client.getBlobAsyncClient(filename);
      blobAsyncClient.upload(data, true).block();
    } catch (Exception e) {
      logger.error("Error: Blob upload ({}) to container '{}' was unsuccessful\n", filename, this.containerName, e);
    }

    logger.info("Blob upload ({}) to container '{}' was successful.", filename, this.containerName);
  }
}
