package com.lantanagroup.link;

import com.lantanagroup.link.config.thsa.THSAConfig;

public interface IDataProcessor {
  void process(byte[] dataContent, FhirDataProvider fhirDataProvider, THSAConfig thsaConfig);
}
