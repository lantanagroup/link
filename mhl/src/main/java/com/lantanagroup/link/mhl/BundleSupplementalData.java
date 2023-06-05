package com.lantanagroup.link.mhl;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.nhsn.FixResourceId;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BundleSupplementalData implements IReportGenerationDataEvent {
  @Autowired
  private FhirDataProvider fhirDataProvider;

  private static final Logger logger = LoggerFactory.getLogger(BundleSupplementalData.class);

  @Override
  public void execute(Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {

    logger.info(String.format("Adding supplemental data from bundle."));
    // Get references to supplemental data bundles
    Collection<Reference> references = data.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource instanceof MeasureReport)
            .map(resource -> (MeasureReport) resource)
            .filter(measureReport -> measureReport.getType() == MeasureReport.MeasureReportType.INDIVIDUAL)
            .filter(measureReport -> measureReport.hasExtension(MHLAggregator.SUPPLEMENTAL_DATA_EXTENSION_URL))
            .map(measureReport -> measureReport.getExtensionByUrl(MHLAggregator.SUPPLEMENTAL_DATA_EXTENSION_URL))
            .map(extension -> (Reference) extension.getValue())
            .collect(Collectors.toList());

    // Retrieve each supplemental data bundle and copy its resources into the submission bundle
    for (Reference reference : references) {
      Bundle bundle = fhirDataProvider.getBundleById(reference.getReference());
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        data.addEntry().setResource(entry.getResource());
      }
    }
  }

  @Override
  public void execute(List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
  }
}
