package com.lantanagroup.link;

import com.lantanagroup.link.config.thsa.THSAConfig;

import java.io.IOException;

public interface IDataProcessor {
  void process(byte[] dataContent, FhirDataProvider fhirDataProvider) throws IOException;
}
