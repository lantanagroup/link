package com.lantanagroup.nandina.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.FhirHelper;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.PIHCQuestionnaireResponseGenerator;
import com.lantanagroup.nandina.QueryReport;
import com.lantanagroup.nandina.TransformHelper;
import com.lantanagroup.nandina.direct.DirectSender;
import com.lantanagroup.nandina.query.IFormQuery;
import com.lantanagroup.nandina.query.IPrepareQuery;
import com.lantanagroup.nandina.query.QueryFactory;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

  @Autowired
  private JsonProperties jsonProperties;

  /**
   * Uses reflection to determine what class should be used to execute the requested query/className, and
   * executes the specified query, returning the result.
   *
   * @param className
   * @param reportDate
   * @param overflowLocations
   * @return
   */
  private void executeFormQuery(String className, Map<String, String> criteria, Map<String, Object> contextData, IGenericClient fhirClient, Authentication authentication) {
    if (className == null || className.isEmpty()) return;

    try {
      IFormQuery executor = QueryFactory.newFormQueryInstance(className, jsonProperties, fhirClient, criteria, contextData);
      executor.execute();
    } catch (ClassNotFoundException ex) {
      logger.error("Could not find class for form query named " + className, ex);
    } catch (Exception ex) {
      logger.error("Failed to execute form query class " + className, ex);
    }

    return;
  }

  private void executePrepareQuery(String className, Map<String, String> criteria, Map<String, Object> contextData, IGenericClient fhirClient, Authentication authentication) {
    if (className == null || className.isEmpty()) return;

    try {
      IPrepareQuery executor = QueryFactory.newPrepareQueryInstance(className, jsonProperties, fhirClient, criteria, contextData);
      executor.execute();
    } catch (ClassNotFoundException ex) {
      logger.error("Could not find class for prepare-query named " + className, ex);
    } catch (Exception ex) {
      logger.error("Failed to execute query class for prepare-query " + className, ex);
    }
  }

  private Map<String, String> getCriteria(HttpServletRequest request, QueryReport report) {
    java.util.Enumeration<String> parameterNames = request.getParameterNames();
    Map<String, String> criteria = new HashMap<>();

    while (parameterNames.hasMoreElements()) {
      String paramName = parameterNames.nextElement();
      String[] paramValues = request.getParameterValues(paramName);
      criteria.put(paramName, String.join(",", paramValues));
    }

    if (report.getDate() != null && !report.getDate().isEmpty()) {
      criteria.put("reportDate", report.getDate());
    }

    return criteria;
  }

  /**
   * This endpoint takes the QueryReport and creates the questionResponse. It then sends email with the json, xml report
   * responses using the DirectSender class.
   * @param report - this is the report data after generate report was clicked
   * @return
   * @throws Exception
   */
  @PostMapping("/api/send")
  public QueryReport sendQuestionnaireResponse(@RequestBody() QueryReport report) throws Exception {
    PIHCQuestionnaireResponseGenerator generator = new PIHCQuestionnaireResponseGenerator(report);
    QuestionnaireResponse questionnaireResponse = generator.generate();
    DirectSender sender = new DirectSender(jsonProperties, ctx);

    if (jsonProperties.getExportFormat().equalsIgnoreCase("json")) {
      sender.sendJSON("QuestionnaireResponse JSON", "Please see the attached questionnaireResponse json file", questionnaireResponse);
    } else if (jsonProperties.getExportFormat().equalsIgnoreCase("xml")) {
      sender.sendXML("QuestionnaireResponse XML", "Please see the attached questionnaireResponse xml file", questionnaireResponse);
    } else if (jsonProperties.getExportFormat().equalsIgnoreCase("csv")) {
      sender.sendCSV("QuestionnaireResponse CSV", "Please see the attached questionnaireResponse csv file", this.convertToCSV(questionnaireResponse));
    }
    return report;
  }

  @PostMapping("/api/query")
  public QueryReport getQuestionnaireResponse(Authentication authentication, HttpServletRequest request, @RequestBody() QueryReport report) throws Exception {
    IGenericClient fhirQueryClient = this.getFhirQueryClient(authentication, request);
    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Map<String, String> criteria = this.getCriteria(request, report);

    logger.debug("Generating report, including criteria: " + criteria.toString());

    HashMap<String, Object> contextData = new HashMap<>();

    contextData.put("report", report);
    contextData.put("fhirQueryClient", fhirQueryClient);
    contextData.put("fhirStoreClient", fhirStoreClient);

    FhirHelper.recordAuditEvent(
            fhirStoreClient,
            authentication,
            ReportController.class.getName(),
            "executeQuery()",
            "Generate Report: " + criteria,
            "Generate Report",
            "Successful Report Generated");

    // Execute the prepare query plugin if configured
    this.executePrepareQuery(jsonProperties.getPrepareQuery(), criteria, contextData, fhirQueryClient, authentication);

    // Execute the form query plugin
    this.executeFormQuery(jsonProperties.getFormQuery(), criteria, contextData, fhirQueryClient, authentication);

    return report;
  }

  private String convertToCSV(QuestionnaireResponse questionnaireResponse) throws TransformerException, FileNotFoundException {
    String xml = this.ctx.newXmlParser().encodeResourceToString(questionnaireResponse);
    TransformHelper transformHelper = new TransformHelper();
    return transformHelper.convert(xml);
  }

  @PostMapping("/api/convert")
  public void convertSimpleReport(@RequestBody() QueryReport report, HttpServletResponse response, Authentication authentication, HttpServletRequest request) throws Exception {
    PIHCQuestionnaireResponseGenerator generator = new PIHCQuestionnaireResponseGenerator(report);
    QuestionnaireResponse questionnaireResponse = generator.generate();
    String responseBody = null;
    IGenericClient fhirQueryClient = this.getFhirQueryClient(authentication, request);

    if (jsonProperties.getExportFormat().equals("json")) {
      responseBody = this.ctx.newJsonParser().encodeResourceToString(questionnaireResponse);
      response.setContentType("application/json");
      response.setHeader("Content-Disposition", "attachment; filename=\"report.json\"");
    } else if (jsonProperties.getExportFormat().equals("xml")) {
      responseBody = this.ctx.newXmlParser().encodeResourceToString(questionnaireResponse);
      response.setContentType("application/xml");
      response.setHeader("Content-Disposition", "attachment; filename=\"report.xml\"");
    } else {
      responseBody = this.convertToCSV(questionnaireResponse);
      response.setContentType("text/plain");
      response.setHeader("Content-Disposition", "attachment; filename=\"report.csv\"");
    }

    FhirHelper.recordAuditEvent(fhirQueryClient, authentication, "report." + jsonProperties.getExportFormat(), "ReportController/convertSimpleReport()",
            "Export to File: " + jsonProperties.getExportFormat() + " format", "Export To File", "Successfully Exported File");

    InputStream is = new ByteArrayInputStream(responseBody.getBytes());
    IOUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }
}
