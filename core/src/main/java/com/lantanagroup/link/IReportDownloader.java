package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.bundler.BundlerConfig;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public interface IReportDownloader {
  void download(String reportId, String downloadType, FhirDataProvider fhirDataProvider, HttpServletResponse response, FhirContext ctx, BundlerConfig config) throws IOException, TransformerException;
}
