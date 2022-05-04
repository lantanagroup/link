package com.lantanagroup.link.api;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.Helper;
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

  public ReportGenerator(ReportContext context, ReportCriteria criteria, ApiConfig config, LinkCredentials user) {
    this.context = context;
    this.criteria = criteria;
    this.user = user;
    this.config = config;
  }


  private static MeasureReport.MeasureReportGroupPopulationComponent getOrCreateGroupAndPopulation(MeasureReport masterReport, MeasureReport.MeasureReportGroupPopulationComponent reportPopulation, MeasureReport.MeasureReportGroupComponent reportGroup) {

    String populationCode = reportPopulation.getCode().getCoding().size() > 0 ? reportPopulation.getCode().getCoding().get(0).getCode() : "";
    String groupCode = reportGroup.getCode().getCoding().size() > 0 ? reportGroup.getCode().getCoding().get(0).getCode() : "";

    MeasureReport.MeasureReportGroupComponent masterReportGroupValue;
    MeasureReport.MeasureReportGroupPopulationComponent masteReportGroupPopulationValue;
    // find the group by code
    Optional<MeasureReport.MeasureReportGroupComponent> masterReportGroup = masterReport.getGroup().stream().filter(group -> group.getCode().getCoding().size() > 0 && group.getCode().getCoding().get(0).getCode().equals(groupCode)).findFirst();
    // if empty find the group without the code
    if (masterReportGroup.isPresent()) {
      masterReportGroupValue = masterReportGroup.get();
    } else {
      masterReportGroupValue = masterReport.getGroup().size() > 0 ? masterReport.getGroup().get(0) : null;
    }
    // if still empty create it
    if (masterReportGroupValue == null) {
      masterReportGroupValue = new MeasureReport.MeasureReportGroupComponent();
      masterReportGroupValue.setCode(reportGroup.getCode() != null ? reportGroup.getCode() : null);
      masterReport.addGroup(masterReportGroupValue);
    }
    // find population by code
    Optional<MeasureReport.MeasureReportGroupPopulationComponent> masterReportGroupPopulation = masterReportGroupValue.getPopulation().stream().filter(population -> population.getCode().getCoding().size() > 0 && population.getCode().getCoding().get(0).getCode().equals(populationCode)).findFirst();
    // if empty create it
    if (masterReportGroupPopulation.isPresent()) {
      masteReportGroupPopulationValue = masterReportGroupPopulation.get();
    } else {
      masteReportGroupPopulationValue = new MeasureReport.MeasureReportGroupPopulationComponent();
      masteReportGroupPopulationValue.setCode(reportPopulation.getCode());
      masterReportGroupValue.addPopulation(masteReportGroupPopulationValue);
    }
    return masteReportGroupPopulationValue;
  }

  private static Resource getOrCreateContainedList(MeasureReport master, String code) {
    // find the list by code
    Optional<Resource> resource = master.getContained().stream().filter(resourceList -> resourceList.getId().contains(code)).findFirst();
    // create the list if not found
    if (!resource.isPresent()) {
      ListResource listResource = new ListResource();
      listResource.setId(code + "-subject-list");
      listResource.setStatus(ListResource.ListStatus.CURRENT);
      listResource.setMode(ListResource.ListMode.SNAPSHOT);
      master.getContained().add(listResource);
      return listResource;
    }
    return resource.get();
  }

  private static void addSubjectResults(MeasureReport.MeasureReportGroupPopulationComponent population, MeasureReport.MeasureReportGroupPopulationComponent measureGroupPopulation) {
    measureGroupPopulation.setSubjectResults(new Reference());
    String populationCode = population.getCode().getCoding().size() > 0 ? population.getCode().getCoding().get(0).getCode() : "";
    measureGroupPopulation.getSubjectResults().setReference("#" + populationCode + "-subject-list");
  }

  private static void addMeasureReportReferences(MeasureReport patientMeasureReport, ListResource listResource) {
    ListResource.ListEntryComponent listEntry = new ListResource.ListEntryComponent();
    listEntry.setItem(new Reference());
    listEntry.getItem().setReference("MeasureReport/" + patientMeasureReport.getId());
    listResource.addEntry(listEntry);
  }

  private static MeasureReport generateMasterMeasureReport(ReportCriteria criteria, ReportContext context, List<MeasureReport> patientMeasureReports) throws ParseException {
    // Create the master measure report
    MeasureReport masterMeasureReport = new MeasureReport();
    masterMeasureReport.setId(context.getReportId());
    masterMeasureReport.setType(MeasureReport.MeasureReportType.SUBJECTLIST);
    masterMeasureReport.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
    masterMeasureReport.setPeriod(new Period());
    masterMeasureReport.getPeriod().setStart(Helper.parseFhirDate(criteria.getPeriodStart()));
    masterMeasureReport.getPeriod().setEnd(Helper.parseFhirDate(criteria.getPeriodEnd()));
    masterMeasureReport.setMeasure(context.getMeasure().getUrl());

    // agregate all individual reports in one
    for (MeasureReport patientMeasureReport : patientMeasureReports) {
      for (MeasureReport.MeasureReportGroupComponent group : patientMeasureReport.getGroup()) {
        for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
          // Check if group and population code exist in master, if not create
          MeasureReport.MeasureReportGroupPopulationComponent measureGroupPopulation = getOrCreateGroupAndPopulation(masterMeasureReport, population, group);
          // Add population.count to the master group/population count
          measureGroupPopulation.setCount(measureGroupPopulation.getCount() + population.getCount());
          // If this population incremented the master
          if (population.getCount() > 0) {
            // add subject results
            addSubjectResults(population, measureGroupPopulation);
            // Identify or create the List for this master group/population
            ListResource listResource = (ListResource) getOrCreateContainedList(masterMeasureReport, population.getCode().getCoding().get(0).getCode());
            // add this patient measure report to the contained List
            addMeasureReportReferences(patientMeasureReport, listResource);
          }
        }
      }
    }
    // if there are no groups generated then gets them from the measure
    if (masterMeasureReport.getGroup().size() == 0) {
      Bundle bundle = context.getReportDefBundle();
      Optional<Bundle.BundleEntryComponent> measureEntry = bundle.getEntry().stream()
              .filter(e -> e.getResource().getResourceType() == ResourceType.Measure)
              .findFirst();

      if (measureEntry.isPresent()) {
        Measure measure = (Measure) measureEntry.get().getResource();
        measure.getGroup().forEach(group -> {
          MeasureReport.MeasureReportGroupComponent groupComponent = new MeasureReport.MeasureReportGroupComponent();
          groupComponent.setCode(group.getCode());
          group.getPopulation().forEach(population -> {
            MeasureReport.MeasureReportGroupPopulationComponent populationComponent = new MeasureReport.MeasureReportGroupPopulationComponent();
            populationComponent.setCode(population.getCode());
            populationComponent.setCount(0);
            groupComponent.addPopulation(populationComponent);
          });
          masterMeasureReport.addGroup(groupComponent);
        });
      }

    }
    return masterMeasureReport;
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
   * It also stores all individual reports and the master measure report on the Fhir Server. If is regenerating it is reusing the already generated Id-s for all document reference, master measure report and individual reports.
   *
   * @param criteria                  - the report criteria
   * @param context                   -  the report context
   * @param queryResponses            - the list of patient id-s and bundle-s to generate reports for
   * @param existingDocumentReference - the existing document reference
   */
  public MeasureReport generateAndStore(ReportCriteria criteria, ReportContext context, List<QueryResponse> queryResponses, DocumentReference existingDocumentReference) throws ParseException {
    if(this.config.getEvaluationService() == null) {
      throw new ConfigurationException("api.evaluation-service has not been configured");
    }

    // Create a bundle to execute as a transaction to update multiple resources at once
    Bundle updateBundle = new Bundle();
    updateBundle.setType(Bundle.BundleType.TRANSACTION);

    // Generate a report for each patient
    List<MeasureReport> patientMeasureReports = queryResponses.stream().map(queryResponse -> {
      MeasureReport patientMeasureReport = MeasureEvaluator.generateMeasureReport(criteria, context, config, queryResponse.getPatientId());
      patientMeasureReport.setId(context.getReportId() + "-" + queryResponse.getPatientId().hashCode());

      updateBundle.addEntry()
              .setResource(patientMeasureReport)
              .setRequest(new Bundle.BundleEntryRequestComponent()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl("MeasureReport/" + patientMeasureReport.getIdElement().getIdPart()));

      return patientMeasureReport;
    }).collect(Collectors.toList());


    // Generate the master measure report
    MeasureReport masterMeasureReport = generateMasterMeasureReport(criteria, context, patientMeasureReports);

    // Save measure report and documentReference
    updateBundle.addEntry()
            .setResource(masterMeasureReport)
            .setRequest(new Bundle.BundleEntryRequestComponent()
                    .setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl("MeasureReport/" + context.getReportId()));

    DocumentReference documentReference = this.generateDocumentReference(this.user, criteria, context, context.getReportId());
    String id = criteria.getReportDefIdentifier() + "-" + criteria.getPeriodStart().substring(0, criteria.getPeriodStart().indexOf("T")) + "-" + criteria.getPeriodEnd().substring(0, criteria.getPeriodStart().indexOf("T")).hashCode();
    UUID documentId = UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
    documentReference.setId(documentId.toString());
    if (existingDocumentReference != null) {
      documentReference.setId(existingDocumentReference.getId());

      Extension existingVersionExt = existingDocumentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl);
      String existingVersion = existingVersionExt.getValue().toString();

      documentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl).setValue(new StringType(existingVersion));

      documentReference.setContent(existingDocumentReference.getContent());

      updateBundle.addEntry()
              .setResource(documentReference)
              .setRequest(new Bundle.BundleEntryRequestComponent()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl("DocumentReference/" + documentReference.getIdElement().getIdPart()));
    } else {
      updateBundle.addEntry()
              .setResource(documentReference)
              .setRequest(new Bundle.BundleEntryRequestComponent()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl("DocumentReference/" + documentReference.getIdElement().getIdPart()));
    }

    // Execute the transaction of updates on the internal FHIR server for MeasureReports and doc ref
    this.context
            .getFhirProvider()
            .transaction(updateBundle);

    return masterMeasureReport;
  }
}
