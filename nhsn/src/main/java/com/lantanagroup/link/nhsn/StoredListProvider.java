package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IPatientIdProvider;
import com.lantanagroup.link.config.api.ApiConfig;
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
  public List<String> getPatientIdentifiers(ReportCriteria criteria, ReportContext context, ApiConfig config) {
    List<String> patientIdentifiers = new ArrayList<>();
    FhirContext ctx = context.getFhirContext();

    Bundle bundle = context.getFhirStoreClient()
            .search()
            .forResource(ListResource.class)
            .and(ListResource.DATE.exactly().day(criteria.getPeriodStart()))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if (bundle.getEntry().size() == 0) {
      logger.info("No patient identifier lists found matching time stamp " + criteria.getPeriodStart());
      return patientIdentifiers;
    }

    List<IBaseResource> bundles = FhirHelper.getAllPages(bundle, context.getFhirStoreClient(), ctx);

    bundles.parallelStream().forEach(bundleResource -> {
      ListResource resource = (ListResource) ctx.newJsonParser().parseResource(ctx.newJsonParser().setPrettyPrint(false).encodeResourceToString(bundleResource));
      List<String> patientResourceIds = resource.getEntry().stream().map((patient) -> patient.getItem().getIdentifier().getSystem()
              + "|"
              + patient.getItem().getIdentifier().getValue()).collect(Collectors.toList());
      patientIdentifiers.addAll(patientResourceIds);
    });

    logger.info("Loaded " + patientIdentifiers.size() + " patient ids");
    patientIdentifiers.forEach(id -> logger.info("PatientId: " + id));
    return patientIdentifiers;
  }
}
