package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.*;
import com.lantanagroup.link.api.MeasureEvaluator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiQueryConfigModes;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.model.*;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.util.Strings;
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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

  private String storeReportBundleResources (Bundle bundle, IGenericClient fhirStoreClient) {
    String measureId = null;

    Optional<Bundle.BundleEntryComponent> measureEntry = bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType() == ResourceType.Measure)
            .findFirst();

    if (measureEntry.isPresent()) {
      measureId = measureEntry.get().getResource().getIdElement().getIdPart();
    }

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

    return measureId;
  }

  private void resolveMeasure (ReportCriteria criteria, ReportContext context) throws Exception {
    // Find the report definition bundle for the given ID
    Bundle reportDefBundle = context.getFhirStoreClient()
            .read()
            .resource(Bundle.class)
            .withId(criteria.getReportDefId())
            .execute();

    if (reportDefBundle == null) {
      throw new Exception("Did not find report definition with ID " + criteria.getReportDefId());
    }

    try {
      // Store the resources in the report definition bundle on the internal FHIR server
      logger.info("Storing the resources for the report definition " + criteria.getReportDefId());
      String measureId = this.storeReportBundleResources(reportDefBundle, context.getFhirStoreClient());
      context.setMeasureId(measureId);
      context.setReportDefBundle(reportDefBundle);
    } catch (Exception ex) {
      logger.error("Error storing resources for the report definition " + criteria.getReportDefId() + ": " + ex.getMessage());
      throw new Exception("Error storing resources for the report definition: " + ex.getMessage());
    }
  }

  private List<String> getPatientIdentifiers (ReportCriteria criteria, ReportContext context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    IPatientIdProvider provider;
    Class<?> senderClass = Class.forName(this.config.getPatientIdResolver());
    Constructor<?> senderConstructor = senderClass.getConstructor();
    provider = (IPatientIdProvider) senderConstructor.newInstance();
    return provider.getPatientIdentifiers(criteria, context, this.config);
  }

  private Bundle getRemotePatientData (List<String> patientIdentifiers) {
    try {
      URL url = new URL(new URL(this.config.getQuery().getUrl()), "/api/data");
      URIBuilder uriBuilder = new URIBuilder(url.toString());
      patientIdentifiers.forEach(patientIdentifier -> uriBuilder.addParameter("patientIdentifier", patientIdentifier));

      logger.info("Scooping data remotely for the patients: " + StringUtils.join(patientIdentifiers, ", ") + " from: " + uriBuilder);

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
        IQuery query = QueryFactory.getQueryInstance(this.context, queryConfig.getQueryClass());
        patientDataBundle = query.execute(patientIdentifiers.toArray(new String[patientIdentifiers.size()]));
      } else if (this.config.getQuery().getMode() == ApiQueryConfigModes.Remote) {
        patientDataBundle = this.getRemotePatientData(patientIdentifiers);
      }

      if (patientDataBundle == null) {
        throw new Exception("patientDataBundle is null");
      }

      // Make sure the bundle is a transaction
      patientDataBundle.setType(Bundle.BundleType.TRANSACTION);
      patientDataBundle.getEntry().forEach(entry ->
              entry.getRequest()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart())
      );

      // Store the data
      logger.info("Storing data for the patients: " + StringUtils.join(patientIdentifiers, ", "));
      fhirStoreClient.transaction().withBundle(patientDataBundle).execute();
    } catch (Exception ex) {
      String msg = String.format("Error scooping/storing data for the patients (%s): %s", StringUtils.join(patientIdentifiers, ", "), ex.getMessage());
      logger.error(msg);
      throw new Exception(msg, ex);
    }
  }

  private DocumentReference getDocumentReferenceByMeasureAndPeriod (Identifier measureIdentifier, String startDate, String endDate, IGenericClient fhirStoreClient, boolean regenerate) throws Exception {
    DocumentReference documentReference = null;
    Bundle bundle = fhirStoreClient
            .search()
            .forResource(DocumentReference.class)
            .where(DocumentReference.IDENTIFIER.exactly().systemAndValues(measureIdentifier.getSystem(), measureIdentifier.getValue()))
            .and(DocumentReference.PERIOD.afterOrEquals().day(startDate))
            .and(DocumentReference.PERIOD.beforeOrEquals().day(endDate))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
    int size = bundle.getEntry().size();
    if (size > 0) {
      if (size == 1) {
        documentReference = (DocumentReference) bundle.getEntry().get(0).getResource();
      } else {
        throw new Exception("We have more than 1 report for the selected measure and report date.");
      }
    }
    return documentReference;
  }

  @PostMapping("/$generate")
  public GenerateResponse generateReport (
          @AuthenticationPrincipal LinkCredentials user,
          Authentication authentication,
          HttpServletRequest request,
          @RequestParam("reportDefId") String reportDefId,
          @RequestParam("periodStart") String periodStart,
          @RequestParam("periodEnd") String periodEnd,
          boolean regenerate) throws Exception {

    GenerateResponse response = new GenerateResponse();
    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    ReportCriteria criteria = new ReportCriteria(reportDefId, periodStart, periodEnd);
    ReportContext context = new ReportContext(fhirStoreClient, this.ctx);

    try {
      // Get the latest measure def and update it on the FHIR storage server
      this.resolveMeasure(criteria, context);

      // Search the reference document by measure criteria nd reporting period
      DocumentReference existingDocumentReference = this.getDocumentReferenceByMeasureAndPeriod(
              context.getReportDefBundle().getIdentifier(),
              criteria.getPeriodStart(),
              criteria.getPeriodEnd(),
              fhirStoreClient,
              regenerate);
      if (existingDocumentReference != null && !regenerate) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "A report has already been generated for the specified measure and reporting period. Are you sure you want to re-generate the report (re-query the data from the EHR and re-evaluate the measure based on updated data)?");
      }

      // Get the patient identifiers for the given date
      List<String> patientIdentifiers = this.getPatientIdentifiers(criteria, context);

      // Scoop the data for the patients and store it
      this.queryAndStorePatientData(patientIdentifiers, fhirStoreClient);

      FhirHelper.recordAuditEvent(request, fhirStoreClient, user.getJwt(), FhirHelper.AuditEventTypes.InitiateQuery, "Successfully Initiated Query");

      // Generate the report id
      String id = "";
      if (!regenerate || Strings.isEmpty(id)) {
        id = RandomStringUtils.randomAlphanumeric(8);
      } else {
        id = null != existingDocumentReference ? existingDocumentReference.getMasterIdentifier().getValue() : "";
      }
      context.setReportId(id);
      response.setReportId(id);
      MeasureReport measureReport = MeasureEvaluator.generateMeasureReport(criteria, context, this.config);

      FhirHelper.recordAuditEvent(request, fhirStoreClient, user.getJwt(), FhirHelper.AuditEventTypes.Generate, "Successfully Generated Report");

      if (measureReport != null) {
        // Save measure report and documentReference
        this.updateResource(measureReport, fhirStoreClient);

        DocumentReference documentReference = this.generateDocumentReference(user, criteria, context, id);
        if (existingDocumentReference != null) {
          documentReference.setId(existingDocumentReference.getId());
          this.updateResource(documentReference, fhirStoreClient);
        } else {
          this.createResource(documentReference, fhirStoreClient);
        }
      }
    } catch (ResponseStatusException rse) {
      logger.error(String.format("Error generating report: %s", rse.getMessage()), rse);
      throw rse;
    } catch (Exception ex) {
      logger.error(String.format("Error generating report: %s", ex.getMessage()), ex);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please contact system administrator regarding this error.");
    }

    return response;
  }

  private DocumentReference generateDocumentReference (LinkCredentials user, ReportCriteria criteria, ReportContext context, String identifierValue) {
    DocumentReference documentReference = new DocumentReference();
    Identifier identifier = new Identifier();
    identifier.setSystem(config.getDocumentReferenceSystem());
    identifier.setValue(identifierValue);

    documentReference.setMasterIdentifier(identifier);
    documentReference.addIdentifier(context.getReportDefBundle().getIdentifier());

    documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
    List<Reference> list = new ArrayList<>();
    Reference reference = new Reference();
    String practitionerId = user.getPractitioner().getId();
    reference.setReference(practitionerId.substring(practitionerId.indexOf("Practitioner"), practitionerId.indexOf("_history") - 1));
    list.add(reference);
    documentReference.setAuthor(list);
    documentReference.setDocStatus(DocumentReference.ReferredDocumentStatus.PRELIMINARY);
    CodeableConcept type = new CodeableConcept();
    List<Coding> codings = new ArrayList<>();
    Coding coding = new Coding();
    coding.setCode(Constants.DocRefCode);
    coding.setSystem(Constants.LoincSystemUrl);
    coding.setDisplay(Constants.DocRefDisplay);
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
    LocalDate startDate = LocalDate.parse(criteria.getPeriodStart());
    LocalDate endDate = LocalDate.parse(criteria.getPeriodEnd());
    Period period = new Period();
    period.setStart(java.sql.Timestamp.valueOf(startDate.atStartOfDay()));
    period.setEnd(java.sql.Timestamp.valueOf(endDate.atTime(23, 59, 59)));
    docReference.setPeriod(period);
    documentReference.setContext(docReference);
    return documentReference;
  }

  /**
   * This endpoint takes the QueryReport and creates the questionResponse. It then sends email with the json, xml report
   * responses using the DirectSender class.
   *
   * @param reportId - this is the report identifier after generate report was clicked
   * @throws Exception Thrown when the configured sender class is not found or fails to initialize or the reportId it not found
   */
  @GetMapping("/{reportId}/$send")
  public void send (Authentication authentication, @PathVariable String reportId, HttpServletRequest request) throws Exception {
    if (StringUtils.isEmpty(this.config.getSender()))
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not configured for sending");

    IReportSender sender;
    Class<?> senderClass = Class.forName(this.config.getSender());
    Constructor<?> senderConstructor = senderClass.getConstructor();
    sender = (IReportSender) senderConstructor.newInstance();

    // get the report
    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Bundle bundle = fhirStoreClient
            .search()
            .forResource(DocumentReference.class)
            .where(DocumentReference.IDENTIFIER.exactly().systemAndValues(config.getDocumentReferenceSystem(), reportId))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
    if (bundle.getTotal() == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The report does not exist.");
    }

    MeasureReport report = fhirStoreClient.read().resource(MeasureReport.class).withId(reportId).execute();
    sender.send(report, this.config, this.ctx, request, authentication, fhirStoreClient);
  }

  @GetMapping("/{reportId}/$download")
  public void download (@PathVariable String reportId, HttpServletResponse response, Authentication authentication, HttpServletRequest request) throws Exception {
    if (StringUtils.isEmpty(this.config.getDownloader()))
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not configured for downloading");

    IReportDownloader downloader;
    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Class<?> downloaderClass = Class.forName(this.config.getDownloader());
    Constructor<?> downloaderCtor = downloaderClass.getConstructor();
    downloader = (IReportDownloader) downloaderCtor.newInstance();

    downloader.download(reportId, fhirStoreClient, response, this.ctx, this.config);

    FhirHelper.recordAuditEvent(request, fhirStoreClient, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Export, "Successfully Exported Report for Download");
  }

  @GetMapping(value = "/{id}")
  public ReportModel getReport(
          @PathVariable("id") String id,
          Authentication authentication,
          HttpServletRequest request) throws Exception {

    IGenericClient client = this.getFhirStoreClient(authentication, request);
    ReportModel report = new ReportModel();

    Bundle documentReferences = client.search()
            .forResource("DocumentReference")
            .where(DocumentReference.IDENTIFIER.exactly().identifier(id))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if (!documentReferences.hasEntry() || documentReferences.getEntry().size() != 1) {
      throw new HttpResponseException(404, String.format("Report with id %s does not exist", id));
    }

    DocumentReference documentReference = (DocumentReference) documentReferences.getEntry().get(0).getResource();

    Bundle measureBundle = client.search()
            .forResource("Measure")
            .where(Measure.IDENTIFIER.exactly().identifier(documentReference.getIdentifier().get(0).getValue()))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    MeasureReport measureReport = client.read()
            .resource(MeasureReport.class)
            .withId(documentReference.getMasterIdentifier().getValue())
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
    report.setMeasureReport(measureReport);

    // Assuming that each measure has a unique identifier (only one measure returned per id)
    report.setMeasure(
            measureBundle.hasEntry() && !measureBundle.getEntry().get(0).isEmpty() ?
            (Measure) measureBundle.getEntry().get(0).getResource() :
            null
    );

    report.setIdentifier(id);
    report.setVersion(null);
    report.setStatus(documentReference.getStatus().toString());
    report.setDate(documentReference.getDate());

    return report;
  }

  @DeleteMapping(value = "/{id}")
  public void deleteReport(
          @PathVariable("id") String id,
          Authentication authentication,
          HttpServletRequest request) throws Exception{
    Bundle deleteRequest = new Bundle();
    IGenericClient client = this.getFhirStoreClient(authentication, request);
    Bundle documentReferences = client.search()
            .forResource("DocumentReference")
            .where(DocumentReference.IDENTIFIER.exactly().identifier(id))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
    if (documentReferences.hasEntry() && !documentReferences.getEntry().get(0).isEmpty()) {
      DocumentReference documentReference = (DocumentReference) documentReferences.getEntry().get(0).getResource();
      // Make sure the bundle is a transaction
      deleteRequest.setType(Bundle.BundleType.TRANSACTION);
      deleteRequest.addEntry().setRequest(new Bundle.BundleEntryRequestComponent());
      deleteRequest.addEntry().setRequest(new Bundle.BundleEntryRequestComponent());
      deleteRequest.getEntry().forEach(entry ->
              entry.getRequest().setMethod(Bundle.HTTPVerb.DELETE)
      );
      String documentReferenceId = documentReference.getId();
      documentReferenceId = documentReferenceId.substring(documentReferenceId.indexOf("/DocumentReference/") + "/DocumentReference/".length(),
              documentReferenceId.indexOf("/_history/"));
      deleteRequest.getEntry().get(0).getRequest().setUrl("MeasureReport/" + documentReference.getMasterIdentifier().getValue());
      deleteRequest.getEntry().get(1).getRequest().setUrl("DocumentReference/" + documentReferenceId);
      client.transaction().withBundle(deleteRequest).execute();
      FhirHelper.recordAuditEvent(request, client, ((LinkCredentials) authentication.getPrincipal()).getJwt(),
              FhirHelper.AuditEventTypes.Export, "Successfully deleted DocumentReference" +
                      documentReferenceId + " and MeasureReport " + documentReference.getMasterIdentifier().getValue());
    }
    else {
      throw new HttpResponseException(500, "Couldn't find DocumentReference with identifier: " + id);
    }
  }

  @GetMapping(value = "/searchReports", produces = {MediaType.APPLICATION_JSON_VALUE})
  public ReportBundle searchReports (Authentication authentication, HttpServletRequest request, @RequestParam(required = false, defaultValue = "1") Integer page, @RequestParam(required = false) String bundleId, @RequestParam(required = false) String author,
                                     @RequestParam(required = false) String identifier, @RequestParam(required = false) String periodStartDate, @RequestParam(required = false) String periodEndDate, @RequestParam(required = false) String docStatus, @RequestParam(required = false) String submittedDate) {
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
          url += "identifier=" + Helper.URLEncode(identifier);
          andCond = true;
        }
        if (periodStartDate != null) {
          if (andCond) {
            url += "&";
          }
          url += "period=ge" + periodStartDate;
          andCond = true;
        }
        if (periodEndDate != null) {
          if (andCond) {
            url += "&";
          }
          url += "period=le" + periodEndDate;
          andCond = true;
        }
        if (docStatus != null) {
          if (andCond) {
            url += "&";
          }
          url += "docStatus=" + docStatus.toLowerCase();
        }
        if (submittedDate != null) {
          if (andCond) {
            url += "&";
          }
          url += "date=" + submittedDate;
        }
      }
      documentReference = fhirStoreClient.fetchResourceFromUrl(Bundle.class, url);

      FhirHelper.recordAuditEvent(request, fhirStoreClient, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.SearchReports, "Successfully Searched Reports");
    } catch (Exception ex) {
      logger.error(String.format("Error searching Reports: %s", ex.getMessage()), ex);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please contact system administrator regarding this error");
    }
    Stream<Report> lst = documentReference.getEntry().parallelStream().map(Report::new);
    ReportBundle reportBundle = new ReportBundle();
    reportBundle.setReportTypeId(bundleId != null ? bundleId : documentReference.getId());
    reportBundle.setList(lst.collect(Collectors.toList()));
    reportBundle.setTotalSize(documentReference.getTotal());
    return reportBundle;
  }
}
