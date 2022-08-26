package com.lantanagroup.link;

import java.io.IOException;
import java.io.InputStream;
import com.azure.core.util.BinaryData;

public interface IAzureBlobStorageSender {
  /**
   * Uploads a file to an Azure container using an InputStream
   *
   * @param filename - name of file to add to the Azure container
   * @param inputStream - stream containing file data
   */
  void upload(String filename, InputStream inputStream) throws IOException;

  /**
   * Uploads a file to an Azure container using a file path.
   *
   * @param filename - name of file to add to the Azure container
   * @param filepath - local filepath to the file to be uploaded
   */
  void upload(String filename, String filepath) throws IOException;

  /**
   * Uploads a file to an Azure container using BinaryData
   *
   * @param filename - name of file to add to the Azure container
   * @param data - file data to be uploaded
   */
  void upload(String filename, BinaryData data) throws IOException;
}
