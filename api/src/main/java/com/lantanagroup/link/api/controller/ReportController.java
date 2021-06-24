package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IReportDownloader;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.QueryReport;
import com.lantanagroup.link.api.MeasureEvaluator;
import com.lantanagroup.link.api.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiMeasureConfig;
import com.lantanagroup.link.config.api.ApiQueryConfigModes;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.model.Report;
import com.lantanagroup.link.model.ReportBundle;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/report")
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
  private ObjectMapper mapper = new ObjectMapper();

  @Autowired
  private ApiConfig config;

  @Autowired
  private ApplicationContext context;

  private void storeLatestMeasure (Bundle bundle, IGenericClient fhirStoreClient) {
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

    logger.info("Executing the measure definition bundle as a transaction on " + this.config.getFhirServerStore());

    fhirStoreClient.transaction().withBundle(bundle).execute();

    logger.info("Measure definition bundle transaction executed successfully...");
  }

  private void resolveMeasure (Map<String, String> criteria, IGenericClient fhirStoreClient, Map<String, Object> contextData) throws Exception {
    String measureConfigId = criteria.get("measureId");
    String measureId = null;
    String measureUrl = null;
    Bundle measureBundle = null;
    Identifier measureIdentifier = new Identifier();

    for (ApiMeasureConfig measureConfig : this.config.getMeasures()) {
      if (measureConfig.getId().equals(measureConfigId)) {
        measureUrl = measureConfig.getUrl();
      }
    }

    if (StringUtils.isNotEmpty(measureUrl)) {
      HttpClient client = HttpClient.newHttpClient();
      logger.info("Getting the latest measure definition for " + measureConfigId + " from URL " + measureUrl);
      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(measureUrl))
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      IParser parser = ctx.newJsonParser();

      try {
        logger.debug("Parsing the measure bundle.");
        measureBundle = parser.parseResource(Bundle.class, response.body());
      } catch (Exception ex) {
        logger.error("Error retrieving latest measure definition from " + measureUrl);
        throw new Exception("Could not retrieve the latest measure definition");
      }

      // Find the ID of the Measure resource in the measure definition bundle
      for (Bundle.BundleEntryComponent entry : measureBundle.getEntry()) {
        if (entry.getResource().getResourceType() == ResourceType.Measure) {
          measureId = entry.getResource().getIdElement().getIdPart();
          measureIdentifier = ((Measure) entry.getResource()).getIdentifier().get(0);
          break;
        }
      }

      if (StringUtils.isEmpty(measureId)) {
        logger.error("Measure definition bundle downloaded from " + measureUrl + " does not have a Measure resource in it");
        throw new Exception("Could not find Measure in measure definition bundle");
      }

      try {
        // store the latest measure onto the cqf-ruler server
        logger.info("Storing the latest measure definition for " + measureConfigId + " as " + measureId + " on FHIR server");
        storeLatestMeasure(measureBundle, fhirStoreClient);
      } catch (Exception ex) {
        logger.error("Error storing the latest measure bundle definition from " + measureUrl + ": " + ex.getMessage());
        throw new Exception("Error storing the latest measure bundle definition: " + ex.getMessage());
      }
    }

    contextData.put("measureId", measureId);
    contextData.put("measureUrl", measureUrl);
    contextData.put("measureBundle", measureBundle);
    contextData.put("measureIdentifier", measureIdentifier);
  }

  private Map<String, String> getCriteria (HttpServletRequest request, QueryReport report) {
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


  private List<String> getPatientIdsFromList (String date, IGenericClient fhirStoreClient) {
    List<String> patientIds = new ArrayList<>();
    List<IBaseResource> bundles = new ArrayList<>();

    Bundle bundle = fhirStoreClient
            .search()
            .forResource(ListResource.class)
            .and(ListResource.DATE.exactly().day(date))
            .returnBundle(Bundle.class)
            .execute();

    if (bundle.getEntry().size() == 0) {
      logger.info("No patient identifier lists found matching time stamp " + date);
      return patientIds;
    }

    bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));

    // Load the subsequent pages
    while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
      bundle = fhirStoreClient
              .loadPage()
              .next(bundle)
              .execute();
      logger.info("Adding next page of bundles...");
      bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));
    }

    bundles.parallelStream().forEach(bundleResource -> {
      ListResource resource = (ListResource) ctx.newJsonParser().parseResource(ctx.newJsonParser().setPrettyPrint(false).encodeResourceToString(bundleResource));
      resource.getEntry().parallelStream().forEach(entry -> {
        String patientId = entry.getItem().getIdentifier().getSystem() +
                "|" + entry.getItem().getIdentifier().getValue();
        patientIds.add(patientId);
      });
    });

    logger.info("Loaded " + patientIds.size() + " patient ids");
    patientIds.forEach(id -> logger.info("PatientId: " + id));
    return patientIds;
  }

  private Bundle getRemotePatientData (List<String> patientIdentifiers) {
    try {
      URL url = new URL(new URL(this.config.getQuery().getUrl()), "/api/data");
      URIBuilder uriBuilder = new URIBuilder(url.toString());
      patientIdentifiers.forEach(patientIdentifier -> uriBuilder.addParameter("patientIdentifier", patientIdentifier));

      logger.info("Scooping data remotely for the patients: " + StringUtils.join(patientIdentifiers, ", ") + " from: " + uriBuilder.toString());

      HttpRequest request = HttpRequest.newBuilder()
              .uri(uriBuilder.build().toURL().toURI())
              .header("Authorization", "Key " + this.config.getQuery().getApiKey())
              .build();
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new Exception(String.format("Response from remote query was %s: %s", response.statusCode(), response.body()));
      }

      String responseBody = response.body();
      return (Bundle) this.ctx.newJsonParser().parseResource(responseBody);
    } catch (Exception ex) {
      logger.error("Error retrieving remote patient data: " + ex.getMessage());
    }

    return null;
  }

  private void queryAndStorePatientData (List<String> patientIdentifiers, IGenericClient fhirStoreClient) throws Exception {
    try {
      Bundle patientDataBundle = null;

      // Get the data
      if (this.config.getQuery().getMode() == ApiQueryConfigModes.Local) {
        logger.info("Scooping data locally for the patients: " + StringUtils.join(patientIdentifiers, ", "));
        QueryConfig queryConfig = this.context.getBean(QueryConfig.class);
        IQuery query = QueryFactory.getQueryInstance(this.context, queryConfig);
        patientDataBundle = query.execute(patientIdentifiers.toArray(new String[patientIdentifiers.size()]));
      } else if (this.config.getQuery().getMode() == ApiQueryConfigModes.Remote) {
        patientDataBundle = this.getRemotePatientData(patientIdentifiers);
      }

      if (patientDataBundle == null) {
        throw new Exception("patientDataBundle is null");
      }


      // Make sure the bundle is a transaction
      patientDataBundle.setType(Bundle.BundleType.TRANSACTION);
      patientDataBundle.getEntry().forEach(entry -> {
        entry.getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart());
      });

      // Store the data
      logger.info("Storing data for the patients: " + StringUtils.join(patientIdentifiers, ", "));
      fhirStoreClient.transaction().withBundle(patientDataBundle).execute();
    } catch (Exception ex) {
      String msg = String.format("Error scooping/storing data for the patients (%s): %s", StringUtils.join(patientIdentifiers, ", "), ex.getMessage());
      logger.error(msg);
      throw new Exception(msg, ex);
    }
  }

  private DocumentReference getDocumentReferenceByMeasureAndPeriod (Identifier measureIdentifier, String startDate, String endDate, IGenericClient fhirStoreClient) {
    DocumentReference documentReference = null;
    Bundle bundle = fhirStoreClient
            .search()
            .forResource(DocumentReference.class)
            .where(DocumentReference.IDENTIFIER.exactly().systemAndValues(measureIdentifier.getSystem(), measureIdentifier.getValue()))
            .and(DocumentReference.PERIOD.afterOrEquals().day(startDate))
            .and(DocumentReference.PERIOD.beforeOrEquals().day(endDate))
            .returnBundle(Bundle.class)
            .execute();
    if (bundle.getEntry().size() > 0) {
      documentReference = (DocumentReference) bundle.getEntry().get(0).getResource();
    }
    return documentReference;
  }

  @PostMapping("/$generate")
  public QueryReport generateReport (@AuthenticationPrincipal LinkCredentials user, Authentication authentication, HttpServletRequest request, @RequestBody() QueryReport report, boolean regenerate) throws Exception {

    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Map<String, String> criteria = this.getCriteria(request, report);

    logger.debug("Generating report, including criteria: " + criteria.toString());

    HashMap<String, Object> contextData = new HashMap<>();

    contextData.put("report", report);
    contextData.put("fhirStoreClient", fhirStoreClient);
    contextData.put("fhirContext", this.ctx);

    try {
      // Get the latest measure def and update it on the FHIR storage server
      this.resolveMeasure(criteria, fhirStoreClient, contextData);

      // Search the reference document by measure criteria nd reporting period
      Identifier measureIdentifier = (Identifier) contextData.get("measureIdentifier");
      QueryReport queryReport = (QueryReport) contextData.get("report");
      String startDate = criteria.get("reportDate");
      String endDate = LocalDate.parse(queryReport.getDate()).plusDays(1).toString();
      DocumentReference existingDocumentReference = this.getDocumentReferenceByMeasureAndPeriod(measureIdentifier, startDate, endDate, fhirStoreClient);
      if (existingDocumentReference != null && !regenerate) {
        throw new HttpResponseException(409, "A report has already been generated for the specified measure and reporting period. Are you sure you want to re-generate the report (re-query the data from the EHR and re-evaluate the measure based on updated data)?");
      }

      // Get the patient identifiers for the given date
      List<String> patientIdentifiers = this.getPatientIdsFromList(criteria.get("reportDate"), fhirStoreClient);

      // Scoop the data for the patients and store it
      this.queryAndStorePatientData(patientIdentifiers, fhirStoreClient);

      FhirHelper.recordAuditEvent(request, fhirStoreClient, user.getJwt(), FhirHelper.AuditEventTypes.InitiateQuery, "Successfully Initiated Query");

      // Generate the report id

      String id = "";
      if (!regenerate) {
        id = RandomStringUtils.randomAlphanumeric(8);
      } else {
        id = null != existingDocumentReference ? existingDocumentReference.getMasterIdentifier().getValue() : "";
      }
      contextData.put("reportId", id);
      MeasureReport measureReport = MeasureEvaluator.generateMeasureReport(criteria, contextData, this.config, fhirStoreClient);

      FhirHelper.recordAuditEvent(request, fhirStoreClient, user.getJwt(), FhirHelper.AuditEventTypes.Generate, "Successfully Generated Report");

      if (measureReport != null) {
        // Save measure report and documentReference
        this.updateResource(measureReport, fhirStoreClient);

        DocumentReference documentReference = this.generateDocumentReference(user, contextData, id);
        if (existingDocumentReference != null) {
          documentReference.setId(existingDocumentReference.getId());
          this.updateResource(documentReference, fhirStoreClient);
        } else {
          this.createResource(documentReference, fhirStoreClient);
        }
      }
    } catch (HttpResponseException ex) {
      logger.error(String.format("Error generating report: %s", ex.getMessage()), ex);
      throw ex;
    } catch (Exception ex) {
      logger.error(String.format("Error generating report: %s", ex.getMessage()), ex);
      throw new HttpResponseException(500, "Please contact system administrator regarding this error.");
    }
    return report;
  }

  private DocumentReference generateDocumentReference (LinkCredentials user, HashMap<String, Object> contextData, String id) {

    DocumentReference documentReference = new DocumentReference();
    Identifier identifier = new Identifier();
    identifier.setSystem(config.getDocumentReferenceSystem());
    identifier.setValue(id);
    documentReference.setMasterIdentifier(identifier);

    Identifier measureId = (Identifier) contextData.get("measureIdentifier");
    documentReference.addIdentifier(measureId);

    documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
    List<Reference> list = new ArrayList<>();
    Reference reference = new Reference();
    reference.setReference("Practitioner/" + user.getPractitioner().getId());
    list.add(reference);
    documentReference.setAuthor(list);
    documentReference.setDocStatus(DocumentReference.ReferredDocumentStatus.PRELIMINARY);
    CodeableConcept type = new CodeableConcept();
    List<Coding> codings = new ArrayList<>();
    Coding coding = new Coding();
    coding.setCode("55186-1");
    coding.setSystem("http://loinc.org");
    coding.setDisplay("Measure Document");
    codings.add(coding);
    type.setCoding(codings);
    documentReference.setType(type);
    List<DocumentReference.DocumentReferenceContentComponent> listDoc = new ArrayList<>();
    DocumentReference.DocumentReferenceContentComponent doc = new DocumentReference.DocumentReferenceContentComponent();
    Attachment attachment = new Attachment();
    attachment.setCreation(new Date());
    doc.setAttachment(attachment);
    listDoc.add(doc);
    documentReference.setContent(listDoc);
    DocumentReference.DocumentReferenceContextComponent docReference = new DocumentReference.DocumentReferenceContextComponent();
    QueryReport queryReport = (QueryReport) contextData.get("report");
    LocalDate startDate = LocalDate.parse(queryReport.getDate());
    LocalDate endDate = LocalDate.parse(queryReport.getDate()).plusDays(1);
    Period period = new Period();
    period.setStart(java.sql.Timestamp.valueOf(startDate.atStartOfDay()));
    period.setEnd(java.sql.Timestamp.valueOf(endDate.atStartOfDay()));
    docReference.setPeriod(period);
    documentReference.setContext(docReference);
    return documentReference;
  }

  /**
   * This endpoint takes the QueryReport and creates the questionResponse. It then sends email with the json, xml report
   * responses using the DirectSender class.
   *
   * @param report - this is the report data after generate report was clicked
   * @throws Exception Thrown when the configured sender class is not found or fails to initialize
   */
  @PostMapping("/send")
  public void send (@RequestBody() QueryReport report) throws Exception {
    if (StringUtils.isEmpty(this.config.getSender()))
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not configured for sending");

    IReportSender sender;
    Class<?> senderClass = Class.forName(this.config.getSender());
    Constructor<?> senderConstructor = senderClass.getConstructor();
    sender = (IReportSender) senderConstructor.newInstance();

    sender.send(report, this.config, this.ctx);
  }

  @PostMapping("/download")
  public void download (@RequestBody() QueryReport report, HttpServletResponse response, Authentication authentication, HttpServletRequest request) throws Exception {
    if (StringUtils.isEmpty(this.config.getDownloader()))
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not configured for downloading");

    IReportDownloader downloader;
    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Class<?> downloaderClass = Class.forName(this.config.getDownloader());
    Constructor<?> downloaderCtor = downloaderClass.getConstructor();
    downloader = (IReportDownloader) downloaderCtor.newInstance();

    downloader.download(report, response, this.ctx, this.config);

    FhirHelper.recordAuditEvent(request, fhirStoreClient, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Export, "Successfully Exported File");
  }

  @GetMapping("/measures")
  public List<ApiMeasureConfig> getMeasureConfigs () throws Exception {
    Map<String, String> measureMap = new HashMap<>();
    List<ApiMeasureConfig> measureConfigs = new ArrayList<>();
    if (null == this.config.getMeasures() || this.config.getMeasures().isEmpty())
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Measures are not configured");

    this.config.getMeasures().forEach(config -> {
      ApiMeasureConfig measureConfig = new ApiMeasureConfig();
      measureConfig.setId(config.getId());
      measureConfig.setName(config.getName());
      measureConfig.setSystem(config.getSystem());
      measureConfig.setValue(config.getValue());
      measureConfigs.add(measureConfig);
    });
    return measureConfigs;
  }

  @GetMapping(value = "/searchReports", produces = {MediaType.APPLICATION_JSON_VALUE})
  public ReportBundle searchReports (Authentication authentication, HttpServletRequest request, @RequestParam(required = false, defaultValue = "1") Integer page, @RequestParam(required = false) String bundleId, @RequestParam(required = false) String author,
                                     @RequestParam(required = false) String identifier, @RequestParam(required = false) String periodStartDate, @RequestParam(required = false) String periodEndDate, @RequestParam(required = false) String docStatus) throws Exception {
    Bundle documentReference;
    boolean andCond = false;
    try {
      IGenericClient fhirStoreClient = this.getFhirStoreClient(null, request);
      String url = this.config.getFhirServerStore();
      if (bundleId != null) {
        url += "?_getpages=" + bundleId + "&_getpagesoffset=" + (page - 1) * 20 + "&_count=20";
      } else {
        if (!url.endsWith("/")) url += "/";
        url += "DocumentReference?";
        if (author != null) {
          url += "author=" + author;
          andCond = true;
        }
        if (identifier != null) {
          if (andCond) {
            url += "&";
          }
          url += "identifier=" + identifier.replace("|", "%7C");
          andCond = true;
        }
        if (periodStartDate != null) {
          if (andCond) {
            url += "&";
          }
          url += "period=gt" + periodStartDate;
          andCond = true;
        }
        if (periodEndDate != null) {
          if (andCond) {
            url += "&";
          }
          url += "period=lt" + periodEndDate;
          andCond = true;
        }
        if (docStatus != null) {
          if (andCond) {
            url += "&";
          }
          url += "docStatus=" + docStatus.toLowerCase();
        }
      }
      documentReference = fhirStoreClient.fetchResourceFromUrl(Bundle.class, url);

      FhirHelper.recordAuditEvent(request, fhirStoreClient, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.SearchReports, "Successfully Searched Reports");
    } catch (Exception ex) {
      logger.error(String.format("Error searching Reports: %s", ex.getMessage()), ex);
      throw new HttpResponseException(500, "Please contact system administrator regarding this error");
    }
    Stream<Report> lst = documentReference.getEntry().parallelStream().map(Report::new);
    ReportBundle reportBundle = new ReportBundle();
    reportBundle.setBundleId(documentReference.getId());
    reportBundle.setList(lst.collect(Collectors.toList()));
    return reportBundle;
  }


}
