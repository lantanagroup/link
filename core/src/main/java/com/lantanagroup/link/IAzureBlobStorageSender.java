package com.lantanagroup.link;

import java.io.IOException;
import java.io.InputStream;


public interface IAzureBlobStorageSender {
  /**
   * Uploads a file to an Azure container using an InputStream
   *
   * @param filename - name of file to add to the Azure container
   * @param inputStream - stream containing file data
   */
  void upload(String filename, InputStream inputStream) throws IOException;
}
