package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IPatientIdProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ListResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StoredListProvider implements IPatientIdProvider {
  private static final Logger logger = LoggerFactory.getLogger(StoredListProvider.class);

  @Override
  public List<PatientOfInterestModel> getPatientsOfInterest(ReportCriteria criteria, ReportContext context, ApiConfig config) {
    List<PatientOfInterestModel> patientsOfInterest = new ArrayList<>();
    FhirContext ctx = context.getFhirProvider().getClient().getFhirContext();

    String system = criteria.getReportDefIdentifier().indexOf("|") > 0 ?
            criteria.getReportDefIdentifier().substring(0, criteria.getReportDefIdentifier().indexOf("|")) :
            Constants.MainSystem;
    String value = criteria.getReportDefIdentifier().indexOf("|") > 0 ?
            criteria.getReportDefIdentifier().substring(criteria.getReportDefIdentifier().indexOf("|") + 1) :
            criteria.getReportDefIdentifier();

    Bundle bundle = context.getFhirProvider().findListByIdentifierAndDate(system, value, criteria.getPeriodStart(), criteria.getPeriodEnd());

    if (bundle.getEntry().size() == 0) {
      logger.info("No patient identifier lists found matching time stamp " + criteria.getPeriodStart() + " and Measure " + context.getMeasureId());
      return patientsOfInterest;
    } else {
      logger.info("Found patient identifier lists  matching time stamp " + criteria.getPeriodStart() + " and Measure " + context.getMeasureId());
    }

    List<IBaseResource> bundles = FhirHelper.getAllPages(bundle, context.getFhirProvider(), ctx);

    bundles.parallelStream().forEach(bundleResource -> {
      //ListResource censusList = (ListResource) ctx.newJsonParser().parseResource(ctx.newJsonParser().setPrettyPrint(false).encodeResourceToString(bundleResource));

      List<PatientOfInterestModel> patientResourceIds = ((ListResource) bundleResource).getEntry().stream().map((patient) -> {

        PatientOfInterestModel poi = new PatientOfInterestModel();
        if (patient.getItem().getIdentifier() != null) {
          poi.setIdentifier(patient.getItem().getIdentifier().getSystem() + "|" + patient.getItem().getIdentifier().getValue());
        }
        if (patient.getItem().getReference() != null) {
          poi.setReference(patient.getItem().getReference());
        }
        return poi;
      }).collect(Collectors.toList());

      patientsOfInterest.addAll(patientResourceIds);
      context.getPatientCensusLists().add((ListResource) bundleResource);
    });

    logger.info("Loaded " + patientsOfInterest.size() + " patient ids");
    patientsOfInterest.forEach(id -> logger.info("PatientId: " + id));

    context.setPatientsOfInterest(patientsOfInterest);

    return patientsOfInterest;
  }
}
