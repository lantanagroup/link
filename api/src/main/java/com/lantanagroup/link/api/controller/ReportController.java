package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.DateClientParam;
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
import lombok.Setter;
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
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@RestController
@RequestMapping("/api/report")
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
  private static final String PeriodStartParamName = "periodStart";
  private static final String PeriodEndParamName = "periodEnd";

  private ObjectMapper mapper = new ObjectMapper();
  private String documentReferenceVersionUrl = "https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-report-version";

  @Autowired
  @Setter
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
    Bundle bundle = (Bundle) context.getFhirStoreClient()
            .search()
            .forResource(Bundle.class)
            .where(Bundle.IDENTIFIER.exactly().systemAndValues(criteria.getReportDefIdentifier().substring(0, criteria.getReportDefIdentifier().indexOf("|")), criteria.getReportDefIdentifier().substring(criteria.getReportDefIdentifier().indexOf("|") + 1)))
            .execute();

    if (bundle == null) {
      throw new Exception("Did not find report definition with ID " + criteria.getReportDefId());
    }

    try {
      // Store the resources in the report definition bundle on the internal FHIR server
      Bundle reportDefBundle = (Bundle) bundle.getEntry().get(0).getResource();
      logger.info("Storing the resources for the report definition " + criteria.getReportDefId());
      String measureId = this.storeReportBundleResources(reportDefBundle, context.getFhirStoreClient());
      context.setMeasureId(measureId);
      context.setReportDefBundle(reportDefBundle);
    } catch (Exception ex) {
      logger.error("Error storing resources for the report definition " + criteria.getReportDefId() + ": " + ex.getMessage());
      throw new Exception("Error storing resources for the report definition: " + ex.getMessage());
    }
  }

  private List<PatientOfInterestModel> getPatientIdentifiers(ReportCriteria criteria, ReportContext context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    IPatientIdProvider provider;
    Class<?> senderClass = Class.forName(this.config.getPatientIdResolver());
    Constructor<?> senderConstructor = senderClass.getConstructor();
    provider = (IPatientIdProvider) senderConstructor.newInstance();
    return provider.getPatientsOfInterest(criteria, context, this.config);
  }

  private Bundle getRemotePatientData(List<PatientOfInterestModel> patientsOfInterest) {
    try {
      URL url = new URL(new URL(this.config.getQuery().getUrl()), "/api/data");
      URIBuilder uriBuilder = new URIBuilder(url.toString());

      patientsOfInterest.forEach(poi -> {
        if (poi.getReference() != null) {
          uriBuilder.addParameter("patientRef", poi.getReference());
        } else if (poi.getIdentifier() != null) {
          uriBuilder.addParameter("patientIdentifier", poi.getIdentifier());
        }
      });

      logger.info("Scooping data remotely for the patients: " + StringUtils.join(patientsOfInterest, ", ") + " from: " + uriBuilder);

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

  private void queryAndStorePatientData(List<PatientOfInterestModel> patientsOfInterest, IGenericClient fhirStoreClient) throws Exception {
    try {
      Bundle patientDataBundle = null;

      // Get the data
      if (this.config.getQuery().getMode() == ApiQueryConfigModes.Local) {
        logger.info("Querying/scooping data for the patients: " + StringUtils.join(patientsOfInterest, ", "));
        QueryConfig queryConfig = this.context.getBean(QueryConfig.class);
        IQuery query = QueryFactory.getQueryInstance(this.context, queryConfig.getQueryClass());
        patientDataBundle = query.execute(patientsOfInterest);
      } else if (this.config.getQuery().getMode() == ApiQueryConfigModes.Remote) {
        patientDataBundle = this.getRemotePatientData(patientsOfInterest);
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
      ResourceIdChanger.changeIds(patientDataBundle);

      // For debugging purposes:
      //String patientDataBundleXml = this.ctx.newXmlParser().encodeResourceToString(patientDataBundle);

      // Store the data
      logger.info("Storing data for the patients: " + StringUtils.join(patientsOfInterest, ", "));
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
      String msg = String.format("Error scooping/storing data for the patients (%s): %s", StringUtils.join(patientsOfInterest, ", "), ex.getMessage());
      logger.error(msg);
      throw new Exception(msg, ex);
    }
  }

  private DocumentReference getDocumentReferenceByMeasureAndPeriod(Identifier measureIdentifier, String startDate, String endDate, IGenericClient fhirStoreClient, boolean regenerate) throws Exception {
    DocumentReference documentReference = null;
    DateClientParam periodStart = new DateClientParam(PeriodStartParamName);
    DateClientParam periodEnd = new DateClientParam(PeriodEndParamName);
    Bundle bundle = fhirStoreClient
            .search()
            .forResource(DocumentReference.class)
            .where(DocumentReference.IDENTIFIER.exactly().systemAndValues(measureIdentifier.getSystem(), measureIdentifier.getValue()))
            .and(periodStart.afterOrEquals().second(startDate))
            .and(periodEnd.beforeOrEquals().second(endDate))
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
          @RequestParam("reportDefIdentifier") String reportDefIdentifier,
          @RequestParam("periodStart") String periodStart,
          @RequestParam("periodEnd") String periodEnd,
          boolean regenerate) throws Exception {

    GenerateResponse response = new GenerateResponse();
    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    ReportCriteria criteria = new ReportCriteria(reportDefIdentifier, periodStart, periodEnd);
    ReportContext context = new ReportContext(fhirStoreClient, this.ctx);

    try {
      // Get the latest measure def and update it on the FHIR storage server
      this.resolveMeasure(criteria, context);

      // Search the reference document by measure criteria nd reporting period
      DocumentReference existingDocumentReference = this.getDocumentReferenceByMeasureAndPeriod(
              context.getReportDefBundle().getIdentifier(),
              periodStart,
              periodEnd,
              fhirStoreClient,
              regenerate);
      if (existingDocumentReference != null && !regenerate) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "A report has already been generated for the specified measure and reporting period. Are you sure you want to re-generate the report (re-query the data from the EHR and re-evaluate the measure based on updated data)?");
      }

      if (existingDocumentReference != null) {
        existingDocumentReference = FhirHelper.incrementMinorVersion(existingDocumentReference);
      }

      // Get the patient identifiers for the given date
      List<PatientOfInterestModel> patientsOfInterest = this.getPatientIdentifiers(criteria, context);

      // Scoop the data for the patients and store it
      this.queryAndStorePatientData(patientsOfInterest, fhirStoreClient);

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

  private DocumentReference generateDocumentReference(LinkCredentials user, ReportCriteria criteria, ReportContext context, String identifierValue) throws ParseException {
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
    Period period = new Period();
    Date startDate = Helper.parseFhirDate(criteria.getPeriodStart());
    Date endDate = Helper.parseFhirDate(criteria.getPeriodEnd());
    period.setStartElement(new DateTimeType(startDate, TemporalPrecisionEnum.MILLI, TimeZone.getDefault()));
    period.setEndElement(new DateTimeType(endDate, TemporalPrecisionEnum.MILLI, TimeZone.getDefault()));

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
    documentReference.setDate(new Date());
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

  @GetMapping(value = "/{reportId}")
  public ReportModel getReport(
          @PathVariable("reportId") String reportId,
          Authentication authentication,
          HttpServletRequest request) throws Exception {

    IGenericClient client = this.getFhirStoreClient(authentication, request);
    ReportModel report = new ReportModel();

    DocumentReference documentReference = FhirHelper.getDocumentReference(client, reportId);

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

    report.setIdentifier(reportId);
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

    for(Extension extension: measureReport.getExtension()){
      if(extension.getUrl().contains("StructureDefinition/nhsnlink-excluded-patient")){
        patientRequest.addEntry().setRequest(new Bundle.BundleEntryRequestComponent());
        int index = patientRequest.getEntry().size() - 1;
        patientRequest.getEntry().get(index).getRequest().setMethod(Bundle.HTTPVerb.GET);
        for(Extension ext : extension.getExtension()){
          if(ext.getUrl().contains("patient")){
            String patientUrl = (ext.getValue().getNamedProperty("reference")).getValues().get(0).toString() + "/_history";
            patientRequest.getEntry().get(index).getRequest().setUrl(patientUrl);
          }
        }

      }
    }

    if (patientRequest.hasEntry()) {
      Bundle patientBundle = client.transaction().withBundle(patientRequest).execute();
      for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
        PatientReportModel report = new PatientReportModel();

        if(entry.getResource() != null){
          if(entry.getResource().getResourceType().toString() == "Patient") {

            Patient patient = (Patient) entry.getResource();

            report = FhirHelper.setPatientFields(patient, false);
          }
          else if(entry.getResource().getResourceType().toString() == "Bundle"){
            //This assumes that the entry right after the DELETE event is the most recent version of the Patient
            //immediately before the DELETE (entry indexed at 1)
            Patient deletedPatient = ((Patient) ((Bundle) entry.getResource()).getEntry().get(1).getResource());

            report = FhirHelper.setPatientFields(deletedPatient, true);

          }
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
  }

  /**
   * Retrieves data (encounters, conditions, etc.) for the specified patient within the specified report.
   *
   * @param reportId       The report id
   * @param patientId      The patient id within the report
   * @param authentication The authenticated user making the request
   * @param request        The HTTP request
   * @return SubjectReportModel
   * @throws Exception
   */
  @GetMapping(value = "/{reportId}/patient/{patientId}")
  public PatientDataModel getPatientData(
          @PathVariable("reportId") String reportId,
          @PathVariable("patientId") String patientId,
          Authentication authentication,
          HttpServletRequest request) throws Exception {

    PatientDataModel data = new PatientDataModel();

    IGenericClient client = this.getFhirStoreClient(authentication, request);

    //Checks to make sure the DocumentReference exists
    FhirHelper.getDocumentReference(client, reportId);

    MeasureReport measureReport = FhirHelper.getMeasureReport(client, reportId);
    List<Reference> evaluatedResources = measureReport.getEvaluatedResource();

    if (evaluatedResources.stream().filter(er -> er.getReference().contains(patientId)).findAny().isEmpty()) {
      if(measureReport.getExtension().get(0).getExtension().stream().filter(ext -> ((Reference)ext.getValue()).getReference().contains(patientId)).findAny().isEmpty()){
        throw new HttpResponseException(404, String.format("Report does not contain a patient with id %s", patientId));
      }
    }

    // Conditions
    Bundle conditionBundle = FhirHelper.getPatientResources(client, Condition.SUBJECT.hasId(patientId), "Condition");
    if (conditionBundle.hasEntry()) {
      List<Condition> conditionList = conditionBundle.getEntry().stream()
              .filter(e -> e.getResource() != null)
              .map(e -> (Condition) e.getResource())
              .collect(Collectors.toList());

      conditionList = conditionList.stream()
              .filter(c -> evaluatedResources.stream()
                      .anyMatch(e ->
                              e.getReference().equals(FhirHelper.getIdFromVersion(c.getId()))))
              .collect(Collectors.toList());


      data.setConditions(conditionList);
    }

    // Medications Requests
    Bundle medRequestBundle = FhirHelper.getPatientResources(client, MedicationRequest.SUBJECT.hasId(patientId), "MedicationRequest");
    if (medRequestBundle.hasEntry()) {
      List<MedicationRequest> medicationRequestList = medRequestBundle.getEntry().stream()
              .filter(e -> e.getResource() != null)
              .map(e -> (MedicationRequest) e.getResource())
              .collect(Collectors.toList());

      medicationRequestList = medicationRequestList.stream()
              .filter(m -> evaluatedResources.stream()
                      .anyMatch(e ->
                              e.getReference().equals(FhirHelper.getIdFromVersion(m.getId()))))
              .collect(Collectors.toList());

      data.setMedicationRequests(medicationRequestList);
    }

    // Observations
    Bundle observationBundle = FhirHelper.getPatientResources(client, Observation.SUBJECT.hasId(patientId), "Observation");
    if (observationBundle.hasEntry()) {
      List<Observation> observationList = observationBundle.getEntry().stream()
              .filter(e -> e.getResource() != null)
              .map(e -> (Observation) e.getResource())
              .collect(Collectors.toList());

      observationList = observationList.stream()
              .filter(o -> evaluatedResources.stream()
                      .anyMatch(e ->
                              e.getReference().equals(FhirHelper.getIdFromVersion(o.getId()))))
              .collect(Collectors.toList());

      data.setObservations(observationList);
    }

    // Procedures
    Bundle procedureBundle = FhirHelper.getPatientResources(client, Procedure.SUBJECT.hasId(patientId), "Procedure");
    if (procedureBundle.hasEntry()) {
      List<Procedure> procedureList = procedureBundle.getEntry().stream()
              .filter(e -> e.getResource() != null)
              .map(e -> (Procedure) e.getResource())
              .collect(Collectors.toList());

      procedureList = procedureList.stream()
              .filter(p -> evaluatedResources.stream()
                      .anyMatch(e ->
                              e.getReference().equals(FhirHelper.getIdFromVersion(p.getId()))))
              .collect(Collectors.toList());

      data.setProcedures(procedureList);
    }

    // Encounters
    Bundle encounterBundle = FhirHelper.getPatientResources(client, Encounter.SUBJECT.hasId(patientId), "Encounter");
    if (encounterBundle.hasEntry()) {
      List<Encounter> encounterList = encounterBundle.getEntry().stream()
              .filter(e -> e.getResource() != null)
              .map(e -> (Encounter) e.getResource())
              .collect(Collectors.toList());

      encounterList = encounterList.stream()
              .filter(eL -> evaluatedResources.stream()
                      .anyMatch(e ->
                              e.getReference().equals(FhirHelper.getIdFromVersion(eL.getId()))))
              .collect(Collectors.toList());

      data.setEncounters(encounterList);
    }

    return data;
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
          url += PeriodStartParamName + "=ge" + periodStartDate;
          andCond = true;
        }
        if (periodEndDate != null) {
          if (andCond) {
            url += "&";
          }
          url += PeriodStartParamName + "=le" + periodEndDate;
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
          Date submittedDateAsDate = Helper.parseFhirDate(submittedDate);
          Date theDayAfterSubmittedDateEnd = Helper.addDays(submittedDateAsDate, 1);
          String theDayAfterSubmittedDateEndAsString = Helper.getFhirDate(theDayAfterSubmittedDateEnd);
          url += "date=ge" + submittedDate + "&date=le" + theDayAfterSubmittedDateEndAsString;
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

  /**
   * Retrieves the DocumentReference and MeasureReport, ensures that each of the excluded Patients in the request
   * are listed in the MeasureReport.evaluatedResources or as "excluded" extensions on the MeasureReport. Creates
   * the excluded extension on the MR for each patient, DELETE's each patient. Re-evaluates the MeasureReport against
   * the Measure. Increments the minor version number of the report in DocumentReference. Stores updates to the
   * DR and MR back to the FHIR server.
   *
   * @param authentication   Authentication information to create an IGenericClient to the internal FHIR store
   * @param request          The HTTP request to create an IGenericClient to the internal FHIR store
   * @param user             The user making the request, for the audit trail
   * @param reportId         The ID of the report to re-evaluate after DELETE'ing/excluding the patients.
   * @param excludedPatients A list of patients to be excluded from the report, including reasons for their exclusion
   * @return A ReportModel that has been updated to reflect the exclusions
   * @throws HttpResponseException
   */
  @PostMapping("/{reportId}/$exclude")
  public ReportModel excludePatients(
          Authentication authentication,
          HttpServletRequest request,
          @AuthenticationPrincipal LinkCredentials user,
          @PathVariable("reportId") String reportId,
          @RequestBody List<ExcludedPatientModel> excludedPatients) throws HttpResponseException {

    IGenericClient fhirStoreClient;

    try {
      fhirStoreClient = this.getFhirStoreClient(authentication, request);
    } catch (Exception ex) {
      logger.error("Error initializing FHIR client", ex);
      throw new HttpResponseException(500, "Internal Server Error");
    }

    DocumentReference reportDocRef = FhirHelper.getDocumentReference(fhirStoreClient, reportId);

    if (reportDocRef == null) {
      throw new HttpResponseException(404, String.format("Report %s not found", reportId));
    }

    MeasureReport measureReport = FhirHelper.getMeasureReport(fhirStoreClient, reportId);

    if (measureReport == null) {
      throw new HttpResponseException(404, String.format("Report %s does not have a MeasureReport", reportId));
    }

    if (excludedPatients == null || excludedPatients.size() == 0) {
      throw new HttpResponseException(400, "Not patients indicated to be excluded");
    }

    Measure measure = FhirHelper.getMeasure(fhirStoreClient, reportDocRef);

    if (measure == null) {
      logger.error(String.format("The measure for report %s no longer exists on the system", reportId));
      throw new HttpResponseException(500, "Internal Server Error");
    }

    Bundle excludeChangesBundle = new Bundle();
    excludeChangesBundle.setType(Bundle.BundleType.TRANSACTION);
    Boolean changedMeasureReport = false;

    for (ExcludedPatientModel excludedPatient : excludedPatients) {
      if (Strings.isEmpty(excludedPatient.getPatientId())) {
        throw new HttpResponseException(400, String.format("Patient ID not provided for all exclusions"));
      }

      if (excludedPatient.getReason() == null || excludedPatient.getReason().isEmpty()) {
        throw new HttpResponseException(400, String.format("Excluded patient ID %s does not specify a reason", excludedPatient.getPatientId()));
      }

      // Find any references to the Patient in the MeasureReport.evaluatedResources
      List<Reference> foundEvaluatedPatient = measureReport.getEvaluatedResource().stream()
              .filter(er -> er.getReference() != null && er.getReference().equals("Patient/" + excludedPatient.getPatientId()))
              .collect(Collectors.toList());
      // Find any extensions that list the Patient has already being excluded
      Boolean foundExcluded = measureReport.getExtension().stream()
              .filter(e -> e.getUrl().equals(Constants.ExcludedPatientExtUrl))
              .anyMatch(e -> e.getExtension().stream()
                      .filter(nextExt -> nextExt.getUrl().equals("patient") && nextExt.getValue() instanceof Reference)
                      .anyMatch(nextExt -> {
                        Reference patientRef = (Reference) nextExt.getValue();
                        return patientRef.getReference().equals("Patient/" + excludedPatient.getPatientId());
                      }));

      // Throw an error if the Patient does not show up in either evaluatedResources or the excluded extensions
      if (foundEvaluatedPatient.size() == 0 && !foundExcluded) {
        throw new HttpResponseException(400, String.format("Patient %s is not included in report %s", excludedPatient.getPatientId(), reportId));
      }

      // Create an extension for the excluded patient on the MeasureReport
      if (!foundExcluded) {
        Extension newExtension = new Extension(Constants.ExcludedPatientExtUrl);
        newExtension.addExtension("patient", new Reference("Patient/" + excludedPatient.getPatientId()));
        newExtension.addExtension("reason", excludedPatient.getReason());
        measureReport.addExtension(newExtension);
        changedMeasureReport = true;

        // Remove the patient from evaluatedResources, or HAPI will throw a referential integrity exception since it's getting (or has been) deleted
        if (foundEvaluatedPatient.size() > 0) {
          foundEvaluatedPatient.forEach(ep -> measureReport.getEvaluatedResource().remove(ep));
        }
      }

      logger.debug(String.format("Checking if patient %s has been deleted already", excludedPatient.getPatientId()));

      try {
        // Try to GET the patient to see if it has already been deleted or not
        fhirStoreClient
                .read()
                .resource(Patient.class)
                .withId(excludedPatient.getPatientId())     // Limit the amount we ask for so it's quick
                .elementsSubset("id")
                .execute();

        logger.debug(String.format("Adding patient %s to list of patients to delete", excludedPatient.getPatientId()));

        // Add a "DELETE" request to the bundle, since it hasn't been deleted
        Bundle.BundleEntryRequestComponent deleteRequest = new Bundle.BundleEntryRequestComponent()
                .setUrl("Patient/" + excludedPatient.getPatientId())
                .setMethod(Bundle.HTTPVerb.DELETE);
        excludeChangesBundle.addEntry().setRequest(deleteRequest);
      } catch (Exception ex) {
        // It's been deleted, just log some debugging info
        logger.debug(String.format("During exclusions for report %s, patient %s is already deleted.", reportId, excludedPatient.getPatientId()));
      }
    }

    if (changedMeasureReport) {
      Bundle.BundleEntryRequestComponent updateMeasureReportReq = new Bundle.BundleEntryRequestComponent()
              .setUrl("MeasureReport/" + reportId)
              .setMethod(Bundle.HTTPVerb.PUT);
      excludeChangesBundle.addEntry()
              .setRequest(updateMeasureReportReq)
              .setResource(measureReport);
    }

    if (excludeChangesBundle.getEntry().size() > 0) {
      logger.debug(String.format("Executing transaction update bundle to delete patients and/or update MeasureReport %s", reportId));

      try {
        fhirStoreClient
                .transaction()
                .withBundle(excludeChangesBundle)
                .execute();
      } catch (Exception ex) {
        logger.error(String.format("Error updating resources for report %s to exclude %s patient(s)", reportId, excludedPatients.size()), ex);
        throw new HttpResponseException(500, "Internal Server Error");
      }
    }

    // Create ReportCriteria to be used by MeasureEvaluator
    ReportCriteria criteria = new ReportCriteria(
            measure.getIdentifier().get(0).getSystem() + "|" + measure.getIdentifier().get(0).getValue(),
            reportDocRef.getContext().getPeriod().getStartElement().asStringValue(),
            reportDocRef.getContext().getPeriod().getEndElement().asStringValue());

    // Create ReportContext to be used by MeasureEvaluator
    ReportContext context = new ReportContext(fhirStoreClient, fhirStoreClient.getFhirContext());
    context.setReportId(measureReport.getIdElement().getIdPart());
    context.setMeasureId(measure.getIdElement().getIdPart());
    context.setFhirContext(fhirStoreClient.getFhirContext());
    context.setFhirStoreClient(fhirStoreClient);

    logger.debug("Re-evaluating measure with state of data on FHIR server");

    // Re-evaluate the MeasureReport, now that the Patient has been DELETE'd from the system
    MeasureReport updatedMeasureReport = MeasureEvaluator.generateMeasureReport(criteria, context, this.config);
    updatedMeasureReport.setId(reportId);
    updatedMeasureReport.setExtension(measureReport.getExtension());    // Copy extensions from the original report before overwriting

    // Increment the version of the report
    FhirHelper.incrementMinorVersion(reportDocRef);

    logger.debug(String.format("Updating DocumentReference and MeasureReport for report %s", reportId));

    // Create a bundle transaction to update the DocumentReference and MeasureReport
    Bundle reportUpdateBundle = new Bundle();
    reportUpdateBundle.setType(Bundle.BundleType.TRANSACTION);
    reportUpdateBundle.addEntry()
            .setRequest(
                    new Bundle.BundleEntryRequestComponent()
                            .setUrl("MeasureReport/" + updatedMeasureReport.getIdElement().getIdPart())
                            .setMethod(Bundle.HTTPVerb.PUT))
            .setResource(updatedMeasureReport);
    reportUpdateBundle.addEntry()
            .setRequest(
                    new Bundle.BundleEntryRequestComponent()
                            .setUrl("DocumentReference/" + reportDocRef.getIdElement().getIdPart())
                            .setMethod(Bundle.HTTPVerb.PUT))
            .setResource(reportDocRef);

    try {
      // Execute the update transaction bundle for MeasureReport and DocumentReference
      fhirStoreClient
              .transaction()
              .withBundle(reportUpdateBundle)
              .execute();
    } catch (Exception ex) {
      logger.error("Error updating DocumentReference and MeasureReport during patient exclusion", ex);
      throw new HttpResponseException(500, "Internal Server Error");
    }

    // Record an audit event that the report has had exclusions
    FhirHelper.recordAuditEvent(request, fhirStoreClient, user.getJwt(), FhirHelper.AuditEventTypes.ExcludePatients, String.format("Excluded %s patients from report %s", excludedPatients.size(), reportId));

    // Create the ReportModel that will be returned
    ReportModel report = new ReportModel();
    report.setMeasureReport(updatedMeasureReport);
    report.setMeasure(measure);
    report.setIdentifier(reportId);
    report.setVersion(reportDocRef
            .getExtensionByUrl(documentReferenceVersionUrl) != null ?
            reportDocRef.getExtensionByUrl(documentReferenceVersionUrl).getValue().toString() : null);
    report.setStatus(reportDocRef.getDocStatus().toString());
    report.setDate(reportDocRef.getDate());

    return report;
  }
}

