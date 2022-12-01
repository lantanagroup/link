package com.lantanagroup.link;

import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    Bundle bundle = createBundle();
    triggerEvent(EventTypes.BeforeBundling, bundle);
    if (config.isIncludeCensuses()) {
      addCensuses(bundle, documentReference);
    }
    for (MeasureReport aggregateMeasureReport : aggregateMeasureReports) {
      addMeasureReports(bundle, aggregateMeasureReport);
    }
    triggerEvent(EventTypes.AfterBundling, bundle);
    cleanEntries(bundle);
    return bundle;
  }

  private Bundle createBundle() {
    Bundle bundle = new Bundle();
    bundle.getMeta()
            .addProfile(Constants.MeasureReportBundleProfileUrl)
            .addTag(Constants.MainSystem, "report", "Report");
    bundle.getIdentifier()
            .setSystem(Constants.IdentifierSystem)
            .setValue(UUID.randomUUID().toString());
    bundle.setType(config.getBundleType());
    bundle.setTimestamp(new Date());
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

  private void cleanEntries(Bundle bundle) {
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      Resource resource = entry.getResource();
      String resourceId = getNonLocalId(resource);
      resource.setId(resourceId);
      resource.setMeta(null);
      if (resource instanceof DomainResource) {
        ((DomainResource) resource).setText(null);
      }
      entry.setFullUrl(String.format("http://nhsnlink.org/fhir/%s", resourceId));
      if (config.getBundleType() == Bundle.BundleType.TRANSACTION
              || config.getBundleType() == Bundle.BundleType.BATCH) {
        entry.getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(resourceId);
      }
    }
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
      bundle.addEntry().setResource(mergedCensus);
    } else {
      for (ListResource census : censuses) {
        logger.debug("Adding census: {}", census.getId());
        bundle.addEntry().setResource(census);
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
    bundle.addEntry().setResource(aggregateMeasureReport);
  }

  private void addIndividualMeasureReport(Bundle bundle, MeasureReport individualMeasureReport) {
    logger.debug("Adding individual measure report: {}", individualMeasureReport.getId());
    bundle.addEntry().setResource(individualMeasureReport);

    // Identify line-level resources
    Map<String, List<Reference>> lineLevelResources = getLineLevelResources(individualMeasureReport);

    // If reifying, retrieve the patient data bundle
    Bundle patientDataBundle;
    if (config.isReifyLineLevelResources()) {
      String individualMeasureReportId = individualMeasureReport.getIdElement().getIdPart();
      String patientDataBundleId = ReportIdHelper.getPatientDataBundleId(individualMeasureReportId);
      patientDataBundle = fhirDataProvider.getBundleById(patientDataBundleId);
    } else {
      patientDataBundle = null;
    }

    // As specified in configuration, move/copy line-level resources
    for (Map.Entry<String, List<Reference>> lineLevelResource : lineLevelResources.entrySet()) {
      String resourceId = lineLevelResource.getKey();
      boolean isLocal = resourceId.startsWith("#");

      // Reify non-contained, non-patient line-level resources
      if (!isLocal && config.isReifyLineLevelResources()) {
        if (patientDataBundle == null || resourceId.startsWith("Patient/")) {
          continue;
        }
        Resource resource = patientDataBundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(_resource -> _resource.getId().equals(resourceId))
                .findFirst()
                .orElse(null);
        if (resource == null) {
          continue;
        }
        if (config.isPromoteLineLevelResources()) {
          bundle.addEntry().setResource(resource);
        } else {
          resource.setId(getLocalId(resource));
          individualMeasureReport.addContained(resource);
          for (Reference reference : lineLevelResource.getValue()) {
            reference.setReference(getLocalId(resource));
          }
        }
      }

      // Promote contained line-level resources
      if (isLocal && config.isPromoteLineLevelResources()) {
        Resource resource = individualMeasureReport.getContained().stream()
                .filter(_resource -> _resource.getId().equals(resourceId))
                .findFirst()
                .orElse(null);
        if (resource == null) {
          continue;
        }
        individualMeasureReport.getContained().remove(resource);
        bundle.addEntry().setResource(resource);
        for (Reference reference : lineLevelResource.getValue()) {
          reference.setReference(getNonLocalId(resource));
        }
      }
    }
  }

  private Map<String, List<Reference>> getLineLevelResources(MeasureReport individualMeasureReport) {
    Stream<Reference> evaluatedResources = individualMeasureReport.getEvaluatedResource().stream();
    Stream<Reference> supplementalDataReferences =
            individualMeasureReport.getExtensionsByUrl(Constants.ExtensionSupplementalData).stream()
                    .map(Extension::getValue)
                    .filter(value -> value instanceof Reference)
                    .map(value -> (Reference) value);
    return Stream.concat(supplementalDataReferences, evaluatedResources)
            .filter(reference -> reference.hasExtension(Constants.ExtensionCriteriaReference))
            .collect(Collectors.groupingBy(Reference::getReference, LinkedHashMap::new, Collectors.toList()));
  }

  private String getLocalId(IBaseResource resource) {
    return String.format("#%s", getIdPart(resource));
  }

  private String getNonLocalId(IBaseResource resource) {
    return String.format("%s/%s", resource.fhirType(), getIdPart(resource));
  }

  private String getIdPart(IBaseResource resource) {
    return resource.getIdElement().getIdPart().replaceAll("^#", "");
  }
}
