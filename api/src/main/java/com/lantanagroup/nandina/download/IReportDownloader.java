package com.lantanagroup.nandina.download;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.QueryReport;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public interface IReportDownloader {
  void download(QueryReport report, HttpServletResponse response, FhirContext ctx, JsonProperties config) throws IOException, TransformerException;
}
