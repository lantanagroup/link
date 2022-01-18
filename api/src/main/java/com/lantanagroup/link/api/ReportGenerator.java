package com.lantanagroup.link.api;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.api.controller.ReportController;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class ReportGenerator {
  private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

  private ReportContext context;
  private ReportCriteria criteria;
  private ApiConfig config;
  private LinkCredentials user;

  public ReportGenerator(ReportContext context, ReportCriteria criteria, ApiConfig config, LinkCredentials user) {
    this.context = context;
    this.criteria = criteria;
    this.config = config;
    this.user = user;
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

  public MeasureReport generateAndStore(List<String> patientIds, String masterReportId, DocumentReference existingDocumentReference) throws HttpResponseException, ParseException {
    // Create a bundle to execute as a transaction to update multiple resources at once
    Bundle updateBundle = new Bundle();
    updateBundle.setType(Bundle.BundleType.TRANSACTION);

    // Generate a report for each patient
    List<MeasureReport> patientMeasureReports = patientIds.stream().map(patientId -> {
      MeasureReport patientMeasureReport = MeasureEvaluator.generateMeasureReport(criteria, context, this.config, patientId);
      patientMeasureReport.setId(masterReportId + "-" + patientId.hashCode());

      updateBundle.addEntry()
              .setResource(patientMeasureReport)
              .setRequest(new Bundle.BundleEntryRequestComponent()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl("MeasureReport/" + patientMeasureReport.getIdElement().getIdPart()));

      return patientMeasureReport;
    }).collect(Collectors.toList());

    // Create the master measure report
    MeasureReport masterMeasureReport = new MeasureReport();
    masterMeasureReport.setId(masterReportId);
    masterMeasureReport.setType(MeasureReport.MeasureReportType.SUBJECTLIST);
    masterMeasureReport.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
    // masterMeasureReport.setPeriod(TODO based on start/end dates in criteria);
    // masterMeasureReport.setMeasure(TODO based on criteria measure url);

    // Aggregate the patient measure reports into the master measure report
    MeasureEvaluator.aggregateMeasureReports(masterMeasureReport, patientMeasureReports);

    if (masterMeasureReport != null) {
      // Save measure report and documentReference
      updateBundle.addEntry()
              .setResource(masterMeasureReport)
              .setRequest(new Bundle.BundleEntryRequestComponent()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl("MeasureReport/" + masterReportId));

      DocumentReference documentReference = this.generateDocumentReference(this.user, criteria, context, masterReportId);

      if (existingDocumentReference != null) {
        documentReference.setId(existingDocumentReference.getId());

        Extension existingVersionExt = existingDocumentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl);
        String existingVersion = existingVersionExt.getValue().toString();

        documentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl).setValue(new StringType(existingVersion));

        updateBundle.addEntry()
                .setResource(documentReference)
                .setRequest(new Bundle.BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("DocumentReference/" + documentReference.getIdElement().getIdPart()));
      } else {
        updateBundle.addEntry()
                .setResource(documentReference)
                .setRequest(new Bundle.BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.POST)
                        .setUrl("DocumentReference"));
      }

      // Execute the transaction of updates on the internal FHIR server for MeasureReports and doc ref
      this.context
              .getFhirProvider()
              .transaction(updateBundle);
    } else {
      logger.error("Measure evaluator returned a null MeasureReport");
      throw new HttpResponseException(500, "Internal Server Error");
    }

    return masterMeasureReport;
  }
}
