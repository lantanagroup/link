package com.lantanagroup.link.thsa;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IDataProcessor;
import com.lantanagroup.link.config.thsa.THSAConfig;
import org.springframework.beans.factory.annotation.Autowired;

public class GenericCSVProcessor implements IDataProcessor {

  @Autowired
  private THSAConfig thsaConfig;

  @Override  
  public void process(byte[] dataContent, FhirDataProvider fhirDataProvider) {

  }
}
