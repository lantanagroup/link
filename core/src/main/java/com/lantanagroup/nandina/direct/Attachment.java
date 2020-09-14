package com.lantanagroup.nandina.direct;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Attachment {
  private InputStream inputStream;
  private String mimeType;
  private String fileName;

  public Attachment(byte[] bytes, String mimeType, String fileName) {
    this.inputStream = new ByteArrayInputStream(bytes);
    this.mimeType = mimeType;
    this.fileName = fileName;
  }

  public Attachment(InputStream inputStream, String mimeType, String fileName) {
    this.inputStream = inputStream;
    this.mimeType = mimeType;
    this.fileName = fileName;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
}
