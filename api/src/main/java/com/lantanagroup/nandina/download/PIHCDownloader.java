package com.lantanagroup.nandina.download;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.PIHCQuestionnaireResponseGenerator;
import com.lantanagroup.nandina.QueryReport;
import com.lantanagroup.nandina.TransformHelper;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PIHCDownloader implements IReportDownloader {
  @Override
  public void download(QueryReport report, HttpServletResponse response, FhirContext ctx, JsonProperties config) throws IOException, TransformerException {
    PIHCQuestionnaireResponseGenerator generator = new PIHCQuestionnaireResponseGenerator(report);
    QuestionnaireResponse questionnaireResponse = generator.generate();
    String responseBody = null;

    if (config.getExportFormat().equals("json")) {
      responseBody = ctx.newJsonParser().encodeResourceToString(questionnaireResponse);
      response.setContentType("application/json");
      response.setHeader("Content-Disposition", "attachment; filename=\"report.json\"");
    } else if (config.getExportFormat().equals("xml")) {
      responseBody = ctx.newXmlParser().encodeResourceToString(questionnaireResponse);
      response.setContentType("application/xml");
      response.setHeader("Content-Disposition", "attachment; filename=\"report.xml\"");
    } else {
      responseBody = TransformHelper.questionnaireResponseToCSV(questionnaireResponse, ctx);
      response.setContentType("text/plain");
      response.setHeader("Content-Disposition", "attachment; filename=\"report.csv\"");
    }

    InputStream is = new ByteArrayInputStream(responseBody.getBytes());
    IOUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }
}
