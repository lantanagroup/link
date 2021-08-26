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
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report")
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
  private ObjectMapper mapper = new ObjectMapper();
  private String documentReferenceVersionUrl = "https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-report-version";

  @Autowired
  private ApiConfig config;

  @Autowired
  private ApplicationContext context;

  private String storeReportBundleResources(Bundle bundle, IGenericClient fhirStoreClient) {
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

  private void resolveMeasure(ReportCriteria criteria, ReportContext context) throws Exception {
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

  private List<String> getPatientIdentifiers(ReportCriteria criteria, ReportContext context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    IPatientIdProvider provider;
    Class<?> senderClass = Class.forName(this.config.getPatientIdResolver());
    Constructor<?> senderConstructor = senderClass.getConstructor();
    provider = (IPatientIdProvider) senderConstructor.newInstance();
    return provider.getPatientIdentifiers(criteria, context, this.config);
  }

  private Bundle getRemotePatientData(List<String> patientIdentifiers) {
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

  private void queryAndStorePatientData(List<String> patientIdentifiers, IGenericClient fhirStoreClient) throws Exception {
    try {
      Bundle patientDataBundle = null;

      // Get the data
      if (this.config.getQuery().getMode() == ApiQueryConfigModes.Local) {
        logger.info("Querying/scooping data for the patients: " + StringUtils.join(patientIdentifiers, ", "));
        QueryConfig queryConfig = this.context.getBean(QueryConfig.class);
        IQuery query = QueryFactory.getQueryInstance(this.context, queryConfig.getQueryClass());
        patientDataBundle = query.execute(patientIdentifiers.toArray(new String[patientIdentifiers.size()]));
      } else if (this.config.getQuery().getMode() == ApiQueryConfigModes.Remote) {
        patientDataBundle = this.getRemotePatientData(patientIdentifiers);
      }

      if (patientDataBundle == null) {
        throw new Exception("patientDataBundle is null");
      }

      // Make sure the bundle is a batch - it will load as much as it can
      patientDataBundle.setType(Bundle.BundleType.BATCH);
      patientDataBundle.getEntry().forEach(entry ->
              entry.getRequest()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart())
      );

      // Fix resource IDs in the patient data bundle that are invalid (longer than 64 characters)
      // (note: this also fixes the references to resources within invalid ids)
      FhirHelper.fixResourceIds(patientDataBundle);

      // For debugging purposes:
      //String patientDataBundleXml = this.ctx.newXmlParser().encodeResourceToString(patientDataBundle);

      // Store the data
      logger.info("Storing data for the patients: " + StringUtils.join(patientIdentifiers, ", "));
      Bundle response = fhirStoreClient.transaction().withBundle(patientDataBundle).execute();

      response.getEntry().stream()
              .filter(e -> e.getResponse() != null && e.getResponse().getStatus() != null && !e.getResponse().getStatus().startsWith("20"))   // 200 or 201
              .forEach(e -> {
                if (e.getResponse().getOutcome() != null) {
                  OperationOutcome outcome = (OperationOutcome) e.getResponse().getOutcome();

                  if (outcome.hasIssue()) {
                    outcome.getIssue().forEach(i -> {
                      logger.error(String.format("Entry in response from storing patient data has error: %s", i.getDiagnostics()));
                    });
                  } else if (outcome.getText() != null && outcome.getText().getDivAsString() != null) {
                    logger.error(String.format("Entry in response from storing patient has issue: %s", outcome.getText().getDivAsString()));
                  }
                } else {
                  logger.error(String.format("An entry in the patient data storage transaction/batch failed without an outcome: %s", e.getResponse().getStatus()));
                }
              });
    } catch (Exception ex) {
      String msg = String.format("Error scooping/storing data for the patients (%s): %s", StringUtils.join(patientIdentifiers, ", "), ex.getMessage());
      logger.error(msg);
      throw new Exception(msg, ex);
    }
  }

  private DocumentReference getDocumentReferenceByMeasureAndPeriod(Identifier measureIdentifier, String startDate, String endDate, IGenericClient fhirStoreClient, boolean regenerate) throws Exception {
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
  public GenerateResponse generateReport(
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

      if(existingDocumentReference != null){
          existingDocumentReference = FhirHelper.incrementMinorVersion(existingDocumentReference);
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

          Extension existingVersionExt = existingDocumentReference.getExtensionByUrl(documentReferenceVersionUrl);
          String existingVersion = existingVersionExt.getValue().toString();

          documentReference.getExtensionByUrl(documentReferenceVersionUrl).setValue(new StringType(existingVersion));
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

  private DocumentReference generateDocumentReference(LinkCredentials user, ReportCriteria criteria, ReportContext context, String identifierValue) {
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

    documentReference.addExtension(FhirHelper.createVersionExtension("0.1"));

    return documentReference;
  }

  /**
   * Sends the specified report to the recipients configured in <strong>api.send-urls</strong>
   *
   * @param reportId - this is the report identifier after generate report was clicked
   * @throws Exception Thrown when the configured sender class is not found or fails to initialize or the reportId it not found
   */
  @GetMapping("/{reportId}/$send")
  public void send(Authentication authentication, @PathVariable String reportId, HttpServletRequest request) throws Exception {
    if (StringUtils.isEmpty(this.config.getSender()))
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not configured for sending");

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
    DocumentReference documentReference = (DocumentReference) bundle.getEntry().get(0).getResource();
    documentReference.setDocStatus(DocumentReference.ReferredDocumentStatus.FINAL);

    documentReference = FhirHelper.incrementMajorVersion(documentReference);

    Class<?> senderClazz = Class.forName(this.config.getSender());
    IReportSender sender = (IReportSender) this.context.getBean(senderClazz);
    sender.send(report, this.ctx, request, authentication, fhirStoreClient);


    String submitterName = FhirHelper.getName(((LinkCredentials) authentication.getPrincipal()).getPractitioner().getName());

    logger.info("MeasureReport with ID " + reportId + " submitted by " + submitterName + " on " + new Date());

    // save the DocumentReference with the Final status
    this.updateResource(documentReference, fhirStoreClient);

    FhirHelper.recordAuditEvent(request, fhirStoreClient, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully Sent Report");
  }

  @GetMapping("/{reportId}/$download")
  public void download(@PathVariable String reportId, HttpServletResponse response, Authentication authentication, HttpServletRequest request) throws Exception {
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

    DocumentReference documentReference = FhirHelper.getDocumentReference(client, id);

    report.setMeasureReport(FhirHelper.getMeasureReport(client, documentReference.getMasterIdentifier().getValue()));

    Bundle measureBundle = client.search()
            .forResource("Measure")
            .where(Measure.IDENTIFIER.exactly().identifier(documentReference.getIdentifier().get(0).getValue()))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    // Assuming that each measure has a unique identifier (only one measure returned per id)
    report.setMeasure(
            measureBundle.hasEntry() && !measureBundle.getEntry().get(0).isEmpty() ?
                    (Measure) measureBundle.getEntry().get(0).getResource() :
                    null
    );

    report.setIdentifier(id);
    report.setVersion(documentReference
            .getExtensionByUrl(documentReferenceVersionUrl) != null ?
            documentReference.getExtensionByUrl(documentReferenceVersionUrl).getValue().toString() : null);
    report.setStatus(documentReference.getDocStatus().toString());
    report.setDate(documentReference.getDate());

    return report;
  }

  @GetMapping(value = "/{id}/patient")
  public List<PatientReportModel> getReportPatients(@PathVariable("id") String id,
                                                    Authentication authentication,
                                                    HttpServletRequest request) throws Exception {

    IGenericClient client = this.getFhirStoreClient(authentication, request);
    List<PatientReportModel> reports = new ArrayList();

    DocumentReference documentReference = FhirHelper.getDocumentReference(client, id);

    MeasureReport measureReport = FhirHelper.getMeasureReport(client, documentReference.getMasterIdentifier().getValue());

    Bundle patientRequest = new Bundle();

    for (Reference reference : measureReport.getEvaluatedResource()) {
      if (reference.getReference().startsWith("Patient/")) {
        patientRequest.addEntry().setRequest(new Bundle.BundleEntryRequestComponent());
        int index = patientRequest.getEntry().size() - 1;
        patientRequest.getEntry().get(index).getRequest().setMethod(Bundle.HTTPVerb.GET);
        patientRequest.getEntry().get(index).getRequest().setUrl(reference.getReference());
      }
    }

    if (patientRequest.hasEntry()) {
      Bundle patientBundle = client.transaction().withBundle(patientRequest).execute();
      for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
        PatientReportModel report = new PatientReportModel();
        Patient patient = (Patient) entry.getResource();

        if (patient.getName().size() > 0) {
          if (patient.getName().get(0).getFamily() != null) {
            report.setLastName(patient.getName().get(0).getFamily());
          }
          if (patient.getName().get(0).getGiven().size() > 0 && patient.getName().get(0).getGiven().get(0) != null) {
            report.setFirstName(patient.getName().get(0).getGiven().get(0).toString());
          }
        }

        if (patient.getBirthDate() != null) {
          report.setDateOfBirth(Helper.getFhirDate(patient.getBirthDate()));
        }

        if (patient.getGender() != null) {
          report.setSex(patient.getGender().toString());
        }

        if (patient.getId() != null) {
          report.setId(patient.getIdElement().getIdPart());
        }

        reports.add(report);
      }
    }

    return reports;
  }

  @PutMapping(value = "/{id}")
  public void saveReport(
          @PathVariable("id") String id,
          Authentication authentication,
          HttpServletRequest request,
          @RequestBody ReportSaveModel data) throws Exception {

    IGenericClient client = this.getFhirStoreClient(authentication, request);

    DocumentReference documentReference = FhirHelper.getDocumentReference(client, id);

    documentReference = FhirHelper.incrementMinorVersion(documentReference);

    try {
      this.updateResource(documentReference, client);
      this.updateResource(data.getMeasureReport(), client);
    } catch (Exception ex) {
      logger.error(String.format("Error saving changes to report: %s", ex.getMessage()));
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error saving changes to report");
    }

    FhirHelper.recordAuditEvent(request, client, ((LinkCredentials) authentication.getPrincipal()).getJwt(),
            FhirHelper.AuditEventTypes.Send, "Successfully updated MeasureReport with id: " +
                    documentReference.getMasterIdentifier().getValue());

    //TODO: Add QuestionnaireResponse update
  }

  @DeleteMapping(value = "/{id}")
  public void deleteReport(
          @PathVariable("id") String id,
          Authentication authentication,
          HttpServletRequest request) throws Exception {
    Bundle deleteRequest = new Bundle();
    IGenericClient client = this.getFhirStoreClient(authentication, request);

    DocumentReference documentReference = FhirHelper.getDocumentReference(client, id);

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

  @GetMapping(value = "/searchReports", produces = {MediaType.APPLICATION_JSON_VALUE})
  public ReportBundle searchReports(Authentication authentication, HttpServletRequest request, @RequestParam(required = false, defaultValue = "1") Integer page, @RequestParam(required = false) String bundleId, @RequestParam(required = false) String author,
                                    @RequestParam(required = false) String identifier, @RequestParam(required = false) String periodStartDate, @RequestParam(required = false) String periodEndDate, @RequestParam(required = false) String docStatus, @RequestParam(required = false) String submittedDate) {
    Bundle documentReference;
    boolean andCond = false;
    ReportBundle reportBundle = new ReportBundle();
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
      List<Report> lst = documentReference.getEntry().parallelStream().map(Report::new).collect(Collectors.toList());
      Collection<String> reportIds = lst.stream().map(report -> report.getId()).collect(Collectors.toList());
      Bundle response = fhirStoreClient
              .search()
              .forResource(MeasureReport.class)
              .where(Resource.RES_ID.exactly().codes(reportIds))
              .returnBundle(Bundle.class)
              .cacheControl(new CacheControlDirective().setNoCache(true))
              .execute();

      response.getEntry().parallelStream().forEach(bundleEntry -> {
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.MeasureReport)) {
          MeasureReport measureReport = (MeasureReport) bundleEntry.getResource();
          Extension extension = measureReport.getExtensionByUrl(Constants.NotesUrl);
          Report foundReport = lst.stream().filter(rep -> rep.getId().equals(measureReport.getIdElement().getIdPart())).findAny().orElse(null);
          if (extension != null && foundReport != null) {
            foundReport.setNote(extension.getValue().toString());
          }
        }
      });
      reportBundle.setReportTypeId(bundleId != null ? bundleId : documentReference.getId());
      reportBundle.setList(lst);
      reportBundle.setTotalSize(documentReference.getTotal());

      FhirHelper.recordAuditEvent(request, fhirStoreClient, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.SearchReports, "Successfully Searched Reports");
    } catch (Exception ex) {
      logger.error(String.format("Error searching Reports: %s", ex.getMessage()), ex);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please contact system administrator regarding this error");
    }

    return reportBundle;
  }
}

