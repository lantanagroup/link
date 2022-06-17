package com.lantanagroup.link;

public interface IDataProcessor {
  void process(byte[] dataContent, FhirDataProvider fhirDataProvider);
}
