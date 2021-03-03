package com.lantanagroup.nandina.controller;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.nandina.DefaultField;
import com.lantanagroup.nandina.FhirHelper;
import com.lantanagroup.nandina.MeasureConfig;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.QueryReport;
import com.lantanagroup.nandina.download.IReportDownloader;
import com.lantanagroup.nandina.query.IFormQuery;
import com.lantanagroup.nandina.query.IPrepareQuery;
import com.lantanagroup.nandina.query.QueryFactory;
import com.lantanagroup.nandina.send.IReportSender;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
  private ObjectMapper mapper = new ObjectMapper();

  @Autowired
  private NandinaConfig nandinaConfig;

  private void storeLatestMeasure(Bundle bundle, IGenericClient fhirQueryClient) {
    logger.info("Generating a Bundle Transaction of the Measure");
    bundle.setType(Bundle.BundleType.TRANSACTION);

    // Make sure each entry in the bundle has a request
    bundle.getEntry().forEach(entry -> {
      if (entry.getRequest() == null) {
        entry.setRequest(new Bundle.BundleEntryRequestComponent());
      }

      if (entry.getResource() != null && entry.getResource().getIdElement() != null && StringUtils.isNotEmpty(entry.getResource().getIdElement().getIdPart())) {
        if (entry.getRequest().getMethod() == null) {
          entry.getRequest().setMethod(Bundle.HTTPVerb.PUT);
        }

        if (StringUtils.isEmpty(entry.getRequest().getUrl())) {
          entry.getRequest().setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart());
        }
      }
    });

    IGenericClient client = ctx.newRestfulGenericClient(this.nandinaConfig.getFhirServerStoreBase());
    logger.info("Executing the measure definition bundle as a transaction on " + this.nandinaConfig.getFhirServerStoreBase());

    client.transaction().withBundle(bundle).execute();
    logger.info("Measure definition bundle transaction executed successfully...");
  }

  private void resolveMeasure(Map<String, String> criteria, IGenericClient targetFhirServer, Map<String, Object> contextData) throws Exception {
    String measureConfigId = criteria.get("measureId");
    String measureId = null;
    String measureUrl = null;
    Bundle measureBundle = null;

    List<MeasureConfig> measureConfigs = mapper.convertValue(this.nandinaConfig.getMeasureConfigs(), new TypeReference<List<MeasureConfig>>() { });

    for (MeasureConfig measureConfig : measureConfigs) {
      if (measureConfig.getId().equals(measureConfigId)) {
        measureUrl = measureConfig.getUrl();
      }
    }

    if (StringUtils.isNotEmpty(measureUrl)) {
      HttpClient client = HttpClient.newHttpClient();
      logger.info("Calling <GET> Request <" + measureUrl + ">");
      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(measureUrl))
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      logger.info("Response statusCode: " + response.statusCode());

      IParser parser = ctx.newJsonParser();

      try {
        measureBundle = parser.parseResource(Bundle.class, response.body());
      } catch (Exception ex) {
        logger.error("Error retrieving latest measure definition from " + measureUrl);
        throw new Exception("Could not retrieve the latest measure definition");
      }

      // Find the ID of the Measure resource in the measure definition bundle
      for (Bundle.BundleEntryComponent entry : measureBundle.getEntry()) {
        if (entry.getResource().getResourceType() == ResourceType.Measure) {
          measureId = entry.getResource().getIdElement().getIdPart();
          break;
        }
      }

      if (StringUtils.isEmpty(measureId)) {
        logger.error("Measure definition bundle downloaded from " + measureUrl + " does not have a Measure resource in it");
        throw new Exception("Could not find Measure in measure definition bundle");
      }
      
      try {
        // store the latest measure onto the cqf-ruler server
        logger.info("Calling storeLatestMeasure()");
        storeLatestMeasure(measureBundle, targetFhirServer);
      } catch (Exception ex) {
        logger.error("Error storing the latest measure bundle definition from " + measureUrl);
        throw new Exception("Error storing the latest measure bundle definition");
      }
    }

    contextData.put("measureId", measureId);
    contextData.put("measureUrl", measureUrl);
    contextData.put("measureBundle", measureBundle);
  }

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
      IFormQuery executor = QueryFactory.newFormQueryInstance(className, nandinaConfig, fhirClient, criteria, contextData);
      executor.execute();
    } catch (ClassNotFoundException ex) {
      logger.error("Could not find class for form query named " + className, ex);
    } catch (Exception ex) {
      logger.error("Failed to execute form query class " + className, ex);
    }

    return;
  }

  private void executePrepareQuery(String className, Map<String, String> criteria, Map<String, Object> contextData, IGenericClient fhirClient, Authentication authentication) throws Exception {
    if (className == null || className.isEmpty()) return;

    try {
      IPrepareQuery executor = QueryFactory.newPrepareQueryInstance(className, nandinaConfig, fhirClient, criteria, contextData);
      executor.execute();
    } catch (ClassNotFoundException ex) {
      logger.error("Could not find class for prepare-query named " + className, ex);
      throw new Exception("Could not find class for prepare-query named " + className);
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw new Exception(ex.getMessage());
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

    if (null != report.getMeasureId() && !report.getMeasureId().isEmpty()) {
      criteria.put("measureId", report.getMeasureId());
    }

    return criteria;
  }

  @PostMapping("/api/query")
  public QueryReport generateReport(Authentication authentication, HttpServletRequest request, @RequestBody() QueryReport report) throws Exception {
    IGenericClient fhirQueryClient = this.getFhirQueryClient(authentication, request);
    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Map<String, String> criteria = this.getCriteria(request, report);

    logger.debug("Generating report, including criteria: " + criteria.toString());

    HashMap<String, Object> contextData = new HashMap<>();

    contextData.put("report", report);
    contextData.put("fhirQueryClient", fhirQueryClient);
    contextData.put("fhirStoreClient", fhirStoreClient);
    contextData.put("fhirContext", this.ctx);
    contextData.put("queryCriteria", this.nandinaConfig.getQueryCriteria());

    FhirHelper.recordAuditEvent(fhirStoreClient, authentication, FhirHelper.AuditEventTypes.Generate, "Successful Report Generated");

    // Get the latest measure def and update it on the FHIR storage server
    this.resolveMeasure(criteria, fhirStoreClient, contextData);

    // Execute the prepare query plugin if configured
    this.executePrepareQuery(nandinaConfig.getPrepareQuery(), criteria, contextData, fhirQueryClient, authentication);

    // Execute the form query plugin
    this.executeFormQuery(nandinaConfig.getFormQuery(), criteria, contextData, fhirQueryClient, authentication);

    if (this.nandinaConfig.getDefaultField() != null) {
      List<DefaultField> defaultFieldList = mapper.convertValue(
              this.nandinaConfig.getDefaultField(),
              new TypeReference<List<DefaultField>>(){}
      );
      defaultFieldList.forEach(field -> {
        if (report.getAnswer("facilityId") == null) {
          report.setAnswer("facilityId", field.getFacilityId());
        }

        if (report.getAnswer("summaryCensusId") == null) {
          report.setAnswer("summaryCensusId", field.getSummaryCensusId());
        }
      });
    }

    return report;
  }

  /**
   * This endpoint takes the QueryReport and creates the questionResponse. It then sends email with the json, xml report
   * responses using the DirectSender class.
   * @param report - this is the report data after generate report was clicked
   * @return
   * @throws Exception
   */
  @PostMapping("/api/send")
  public void send(@RequestBody() QueryReport report) throws Exception {
    if (StringUtils.isEmpty(this.nandinaConfig.getSender()))
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not configured for sending");

    IReportSender sender;
    Class<?> senderClass = Class.forName(this.nandinaConfig.getSender());
    Constructor<?> senderConstructor = senderClass.getConstructor();
    sender = (IReportSender) senderConstructor.newInstance();

    sender.send(report, this.nandinaConfig, this.ctx);
  }

  @PostMapping("/api/download")
  public void download(@RequestBody() QueryReport report, HttpServletResponse response, Authentication authentication, HttpServletRequest request) throws Exception {
    if (StringUtils.isEmpty(this.nandinaConfig.getDownloader()))
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not configured for downloading");

    IReportDownloader downloader;
    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Class<?> downloaderClass = Class.forName(this.nandinaConfig.getDownloader());
    Constructor<?> downloaderCtor = downloaderClass.getConstructor();
    downloader = (IReportDownloader) downloaderCtor.newInstance();

    downloader.download(report, response, this.ctx, this.nandinaConfig);

    FhirHelper.recordAuditEvent(fhirStoreClient, authentication, FhirHelper.AuditEventTypes.Export, "Successfully Exported File");
  }

  @GetMapping("/api/report/measures")
  public List<MeasureConfig> getMeasureConfigs() throws Exception {
    Map<String, String> measureMap = new HashMap<>();
    List<MeasureConfig> measureConfigs = new ArrayList<>();
    if (null == nandinaConfig.getMeasureConfigs() || nandinaConfig.getMeasureConfigs().isEmpty())
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Measures are not configured");

    List<MeasureConfig> measureList = mapper.convertValue(
            nandinaConfig.getMeasureConfigs(),
            new TypeReference<List<MeasureConfig>>(){});
    measureList.forEach(config -> {
      MeasureConfig measureConfig = new MeasureConfig();
      measureConfig.setId(config.getId());
      measureConfig.setName(config.getName());
      measureConfigs.add(measureConfig);
    });
    return measureConfigs;
  }
}
