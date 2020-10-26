package com.lantanagroup.nandina.download;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.QueryReport;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PillboxDownloader implements IReportDownloader {
  @Override
  public void download(QueryReport report, HttpServletResponse response, FhirContext ctx, JsonProperties config) throws IOException, TransformerException {
    response.setContentType("text/plain");
    response.setHeader("Content-Disposition", "attachment; filename=\"report.txt\"");
    InputStream is = new ByteArrayInputStream("This is a test".getBytes());
    IOUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }
}
