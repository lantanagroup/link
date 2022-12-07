package com.lantanagroup.link;

import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class FhirBundler {
  protected static final Logger logger = LoggerFactory.getLogger(FhirBundler.class);
  private final BundlerConfig config;
  private final FhirDataProvider fhirDataProvider;
  private final EventService eventService;

  public FhirBundler(BundlerConfig config, FhirDataProvider fhirDataProvider, EventService eventService) {
    this.config = config;
    this.fhirDataProvider = fhirDataProvider;
    this.eventService = eventService;
  }

  public FhirBundler(BundlerConfig config, FhirDataProvider fhirDataProvider) {
    this(config, fhirDataProvider, null);
  }

  public Bundle generateBundle(Collection<MeasureReport> aggregateMeasureReports, DocumentReference documentReference) {
    Bundle bundle = new Bundle();
    bundle.getMeta()
            .addProfile(Constants.MeasureReportBundleProfileUrl)
            .addTag(Constants.MainSystem, "report", "Report");
    bundle.getIdentifier()
            .setSystem(Constants.IdentifierSystem)
            .setValue(UUID.randomUUID().toString());
    bundle.setType(config.getBundleType());
    bundle.setTimestamp(new Date());
    triggerEvent(EventTypes.BeforeBundling, bundle);
    if (config.isIncludeCensuses()) {
      addCensuses(bundle, documentReference);
    }
    for (MeasureReport aggregateMeasureReport : aggregateMeasureReports) {
      addMeasureReports(bundle, aggregateMeasureReport);
    }
    triggerEvent(EventTypes.AfterBundling, bundle);
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      Resource resource = entry.getResource();
      String resourceId = String.format("%s/%s", resource.getResourceType(), resource.getIdElement().getIdPart());
      entry.setFullUrl(String.format("http://nhsnlink.org/fhir/%s", resourceId));
      if (config.getBundleType() == Bundle.BundleType.TRANSACTION
              || config.getBundleType() == Bundle.BundleType.BATCH) {
        entry.getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(resourceId);
      }
    }
    return bundle;
  }

  private void triggerEvent(EventTypes eventType, Bundle bundle) {
    if (eventService == null) {
      return;
    }
    try {
      eventService.triggerDataEvent(eventType, bundle);
    } catch (Exception e) {
      logger.error(String.format("Error occurred in %s handler", eventType), e);
    }
  }

  private Bundle.BundleEntryComponent addOrGetEntry(Bundle bundle, Resource resource) {
    IdType copiedResourceId = new IdType(
            resource.getResourceType().name(),
            resource.getIdElement().getIdPart().replaceAll("^#", ""));
    Optional<Bundle.BundleEntryComponent> existingEntry = bundle.getEntry().stream()
            .filter(entry -> entry.getResource().getIdElement().equals(copiedResourceId))
            .findFirst();
    if (existingEntry.isPresent()) {
      return existingEntry.get();
    }
    Resource copiedResource = resource.copy();
    copiedResource.setId(copiedResourceId);
    copiedResource.setMeta(null);
    if (copiedResource instanceof DomainResource) {
      ((DomainResource) copiedResource).setText(null);
    }
    return bundle.addEntry().setResource(copiedResource);
  }

  private void addCensuses(Bundle bundle, DocumentReference documentReference) {
    logger.debug("Adding censuses");
    Collection<ListResource> censuses = FhirHelper.getCensusLists(documentReference, fhirDataProvider);
    if (censuses.isEmpty()) {
      return;
    }
    if (censuses.size() > 1 && config.isMergeCensuses()) {
      logger.debug("Merging censuses");
      ListResource mergedCensus = censuses.iterator().next().copy();
      mergedCensus.setId(UUID.randomUUID().toString());
      mergedCensus.getEntry().clear();
      for (ListResource census : censuses) {
        FhirHelper.mergeCensusLists(mergedCensus, census);
      }
      addOrGetEntry(bundle, mergedCensus);
    } else {
      for (ListResource census : censuses) {
        logger.debug("Adding census: {}", census.getId());
        addOrGetEntry(bundle, census);
      }
    }
  }

  private void addMeasureReports(Bundle bundle, MeasureReport aggregateMeasureReport) {
    logger.debug("Adding measure reports: {}", aggregateMeasureReport.getMeasure());
    addAggregateMeasureReport(bundle, aggregateMeasureReport);
    if (!config.isIncludeIndividualMeasureReports()) {
      return;
    }
    Collection<MeasureReport> individualMeasureReports = FhirHelper.getPatientReports(
            FhirHelper.getPatientMeasureReportReferences(aggregateMeasureReport),
            fhirDataProvider);
    for (MeasureReport individualMeasureReport : individualMeasureReports) {
      if (individualMeasureReport == null) {
        continue;
      }
      addIndividualMeasureReport(bundle, individualMeasureReport);
    }
  }

  private void addAggregateMeasureReport(Bundle bundle, MeasureReport aggregateMeasureReport) {
    logger.debug("Adding aggregate measure report: {}", aggregateMeasureReport.getId());
    addOrGetEntry(bundle, aggregateMeasureReport);
  }

  private void addIndividualMeasureReport(Bundle bundle, MeasureReport individualMeasureReport) {
    logger.debug("Adding individual measure report: {}", individualMeasureReport.getId());
    individualMeasureReport.getEvaluatedResource().clear();
    if (config.isPromoteContainedResources()) {
      for (Resource containedResource : individualMeasureReport.getContained()) {
        Bundle.BundleEntryComponent entry = addOrGetEntry(bundle, containedResource);
        individualMeasureReport.addEvaluatedResource().setReferenceElement(entry.getResource().getIdElement());
      }
      individualMeasureReport.getContained().clear();
    } else {
      for (Resource containedResource : individualMeasureReport.getContained()) {
        individualMeasureReport.addEvaluatedResource().setReferenceElement(containedResource.getIdElement());
      }
    }
    addOrGetEntry(bundle, individualMeasureReport);
  }
}
