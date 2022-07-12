package com.lantanagroup.link.api;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.IReportAggregator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.QueryResponse;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class creates a master measure report based on every individual report generated for each patient included in the "census" list..
 */
public class ReportGenerator {
  private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

  private ReportContext context;
  private ReportCriteria criteria;
  private LinkCredentials user;
  private ApiConfig config;
  private IReportAggregator reportAggregator;

  public ReportGenerator(ReportContext context, ReportCriteria criteria, ApiConfig config, LinkCredentials user, IReportAggregator reportAggregator) {
    this.context = context;
    this.criteria = criteria;
    this.user = user;
    this.config = config;
    this.reportAggregator = reportAggregator;
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
    coding.setCode(com.lantanagroup.link.Constants.DocRefCode);
    coding.setSystem(com.lantanagroup.link.Constants.LoincSystemUrl);
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
   * This method accepts a list of patients and generates an individual measure report for each patient. Then agregates all the individual reports into a master measure report.
   *
   * @param criteria       - the report criteria
   * @param context        -  the report context
   * @param queryResponses - the list of patient id-s and bundle-s to generate reports for
   */
  public List<MeasureReport> generate(ReportCriteria criteria, ReportContext context, List<QueryResponse> queryResponses) throws ParseException {
    if (this.config.getEvaluationService() == null) {
      throw new ConfigurationException("api.evaluation-service has not been configured");
    }

    // Generate a report for each patient
    List<MeasureReport> patientMeasureReports = queryResponses.stream().map(queryResponse -> {
      MeasureReport patientMeasureReport = MeasureEvaluator.generateMeasureReport(criteria, context, config, queryResponse.getPatientId());
      patientMeasureReport.setId(context.getReportId() + "-" + queryResponse.getPatientId().hashCode());
      return patientMeasureReport;
    }).collect(Collectors.toList());

    MeasureReport masterMeasureReport = reportAggregator.generate(criteria, context, patientMeasureReports);
    context.setMeasureReport(masterMeasureReport);
    return patientMeasureReports;
  }

  /**
   * It also stores all individual reports and the master measure report on the Fhir Server. If is regenerating it is reusing the already generated Id-s for all document reference, master measure report and individual reports.
   * @param existingDocumentReference - the existing document reference
   **/
  public void store(List<MeasureReport> patientMeasureReports, ReportCriteria criteria, ReportContext context, DocumentReference existingDocumentReference) throws ParseException {

    Bundle updateBundle = new Bundle();
    updateBundle.setType(Bundle.BundleType.TRANSACTION);

    patientMeasureReports.stream().map(patientMeasureReport -> {

      updateBundle.addEntry()
              .setResource(patientMeasureReport)
              .setRequest(new Bundle.BundleEntryRequestComponent()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl("MeasureReport/" + patientMeasureReport.getIdElement().getIdPart()));

      return patientMeasureReport;
    }).collect(Collectors.toList());

    // Generate the master measure report
    reportAggregator.generate(criteria, context, patientMeasureReports);

    // Save measure report and documentReference
    updateBundle.addEntry()
            .setResource(context.getMeasureReport())
            .setRequest(new Bundle.BundleEntryRequestComponent()
                    .setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl("MeasureReport/" + context.getReportId()));

    DocumentReference documentReference = this.generateDocumentReference(this.user, criteria, context, context.getReportId());

    if (existingDocumentReference != null) {
      documentReference.setId(existingDocumentReference.getId());

      Extension existingVersionExt = existingDocumentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl);
      String existingVersion = existingVersionExt.getValue().toString();

      documentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl).setValue(new StringType(existingVersion));

      documentReference.setContent(existingDocumentReference.getContent());
    } else {
      // generate document reference id based on the report date range and the measure used in the report generation
      String id = criteria.getReportDefIdentifier() + "-" + criteria.getPeriodStart() + "-" + criteria.getPeriodEnd().hashCode();
      UUID documentId = UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
      documentReference.setId(documentId.toString());
    }

    // Add the patient census list(s) to the document reference
    documentReference.getContext().getRelated().clear();
    documentReference.getContext().getRelated().addAll(this.context.getPatientCensusLists().stream().map(censusList -> new Reference()
              .setReference("List/" + censusList.getIdElement().getIdPart())).collect(Collectors.toList()));

    updateBundle.addEntry()
            .setResource(documentReference)
            .setRequest(new Bundle.BundleEntryRequestComponent()
                    .setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl("DocumentReference/" + documentReference.getIdElement().getIdPart()));

    // Execute the transaction of updates on the internal FHIR server for MeasureReports and doc ref
    this.context.getFhirProvider().transaction(updateBundle);

  }
}
