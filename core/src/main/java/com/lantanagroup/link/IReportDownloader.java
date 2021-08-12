package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.config.api.ApiConfig;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public interface IReportDownloader {
  void download(String reportId, IGenericClient fhirStoreClient, HttpServletResponse response, FhirContext ctx, ApiConfig config) throws IOException, TransformerException;
}
