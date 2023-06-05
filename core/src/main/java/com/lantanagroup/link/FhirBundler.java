package com.lantanagroup.link;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FhirBundler {
  protected static final Logger logger = LoggerFactory.getLogger(FhirBundler.class);
  private static final List<String> SUPPLEMENTAL_DATA_EXTENSION_URLS = List.of(
          "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData",
          "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.supplementalDataElement.reference");
  private final BundlerConfig config;
  private final FhirDataProvider fhirDataProvider;
  private final EventService eventService;
  private Organization org;

  public FhirBundler(BundlerConfig config, FhirDataProvider fhirDataProvider, EventService eventService) {
    this.config = config;
    this.fhirDataProvider = fhirDataProvider;
    this.eventService = eventService;
    this.org = this.createOrganization();
  }

  public FhirBundler(BundlerConfig config, FhirDataProvider fhirDataProvider) {
    this(config, fhirDataProvider, null);
  }

  public Bundle generateBundle(Collection<MeasureReport> aggregateMeasureReports, DocumentReference documentReference) {
    Bundle bundle = this.createBundle();
    bundle.addEntry().setResource(this.org);

    triggerEvent(EventTypes.BeforeBundling, bundle);

    if (this.config.isIncludeCensuses()) {
      this.addCensuses(bundle, documentReference);
    }

    for (MeasureReport aggregateMeasureReport : aggregateMeasureReports) {
      this.addMeasureReports(bundle, aggregateMeasureReport);
    }

    triggerEvent(EventTypes.AfterBundling, bundle);

    cleanEntries(bundle);
    return bundle;
  }

  private Organization createOrganization() {
    Organization org = new Organization();
    org.getMeta().addProfile(Constants.QiCoreOrganizationProfileUrl);

    if (!StringUtils.isEmpty(this.config.getOrgNpi())) {
      org.setId("" + this.config.getOrgNpi().hashCode());
    } else {
      org.setId(UUID.randomUUID().toString());
    }

    org.addType()
            .addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
            .setCode("prov")
            .setDisplay("Healthcare Provider");

    if (!StringUtils.isEmpty(this.config.getOrgName())) {
      org.setName(this.config.getOrgName());
    }

    if (!StringUtils.isEmpty(this.config.getOrgNpi())) {
      org.addIdentifier()
              .setSystem(Constants.NationalProviderIdentifierSystemUrl)
              .setValue(this.config.getOrgNpi());
    }

    if (!StringUtils.isEmpty(this.config.getOrgPhone())) {
      org.addTelecom()
              .setSystem(ContactPoint.ContactPointSystem.PHONE)
              .setValue(this.config.getOrgPhone());
    }

    if (!StringUtils.isEmpty(this.config.getOrgEmail())) {
      org.addTelecom()
              .setSystem(ContactPoint.ContactPointSystem.EMAIL)
              .setValue(this.config.getOrgEmail());
    }

    if (this.config.getOrgAddress() != null) {
      org.addAddress(this.config.getOrgAddress().getFHIRAddress());
    }

    return org;
  }

  private Bundle createBundle() {
    Bundle bundle = new Bundle();
    bundle.getMeta()
            .addProfile(config.isMHL() ? Constants.MHLReportBundleProfileUrl : Constants.ReportBundleProfileUrl)
            .addTag((config.isMHL() ? Constants.MHLSystem : Constants.MainSystem), "report", "Report");
    bundle.getIdentifier()
            .setSystem(Constants.IdentifierSystem)
            .setValue("urn:uuid:" + UUID.randomUUID().toString());
    bundle.setType(config.getBundleType());
    bundle.setTimestamp(new Date());
    return bundle;
  }

  private void triggerEvent(EventTypes eventType, Bundle bundle) {
    if (eventService == null) {
      return;
    }
    try {
      eventService.triggerDataEvent(eventType, bundle, null, null, null);
    } catch (Exception e) {
      logger.error(String.format("Error occurred in %s handler", eventType), e);
    }
  }

  private void setProfile(Resource resource) {
    String profile = null;

    switch (resource.getResourceType()) {
      case Patient:
        profile = Constants.QiCorePatientProfileUrl;
        break;
      case Encounter:
        profile = Constants.UsCoreEncounterProfileUrl;
        break;
      case MedicationRequest:
        profile = Constants.UsCoreMedicationRequestProfileUrl;
        break;
      case Medication:
        profile = Constants.UsCoreMedicationProfileUrl;
        break;
      case Condition:
        profile = Constants.UsCoreConditionProfileUrl;
        break;
      case Observation:
        profile = Constants.UsCoreObservationProfileUrl;
        break;
    }

    if (!StringUtils.isEmpty(profile)) {
      String finalProfile = profile;
      if (!resource.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(finalProfile))) {   // Don't duplicate profile if it already exists
        resource.getMeta().addProfile(profile);
      }
    }
  }

  private void addEntry(Bundle bundle, Resource resource, boolean overwrite) {
    String resourceId = getNonLocalId(resource);
    Bundle.BundleEntryComponent entry = bundle.getEntry().stream()
            .filter(_entry -> getNonLocalId(_entry.getResource()).equals(resourceId))
            .findFirst()
            .orElse(null);
    if (entry == null) {
      this.setProfile(resource);
      bundle.addEntry().setResource(resource);
    } else if (overwrite) {
      entry.setResource(resource);
    }
  }

  private void cleanEntries(Bundle bundle) {
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      Resource resource = entry.getResource();
      String resourceId = getNonLocalId(resource);
      resource.setId(resourceId);

      // Only allow meta.profile
      if (resource.getMeta() != null) {
        Meta cleanedMeta = new Meta();
        cleanedMeta.setProfile(resource.getMeta().getProfile());
        resource.setMeta(cleanedMeta);
      }

      if (resource instanceof DomainResource) {
        ((DomainResource) resource).setText(null);
      }
      entry.setFullUrl(String.format("http://lantanagroup.com/fhir/" +
              (config.isMHL() ? "nih-measures" : "nhsn-measures") + "/%s", resourceId));
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
        census.setMeta(new Meta());
        census.getMeta().addProfile(config.isMHL() ? Constants.MHLCensusProfileUrl : Constants.CensusProfileUrl);
        FhirHelper.mergeCensusLists(mergedCensus, census);
      }
      bundle.addEntry().setResource(mergedCensus);
    } else {
      for (ListResource census : censuses) {
        logger.debug("Adding census: {}", census.getId());
        census.setMeta(new Meta());
        census.getMeta().addProfile(config.isMHL() ? Constants.MHLCensusProfileUrl : Constants.CensusProfileUrl);
        bundle.addEntry().setResource(census);
      }
    }
  }

  private void addMeasureReports(Bundle bundle, MeasureReport aggregateMeasureReport) {
    logger.debug("Adding measure reports: {}", aggregateMeasureReport.getMeasure());

    this.addAggregateMeasureReport(bundle, aggregateMeasureReport);

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

      individualMeasureReport.getContained().forEach(c -> this.setProfile(c));  // Ensure all contained resources have the right profiles
      this.addIndividualMeasureReport(bundle, individualMeasureReport);
    }
  }

  private void addAggregateMeasureReport(Bundle bundle, MeasureReport aggregateMeasureReport) {
    logger.debug("Adding aggregate measure report: {}", aggregateMeasureReport.getId());

    // Set the reporter to the facility/org
    aggregateMeasureReport.setReporter(new Reference().setReference("Organization/" + this.org.getIdElement().getIdPart()));

    bundle.addEntry().setResource(aggregateMeasureReport);
  }

  private void addIndividualMeasureReport(Bundle bundle, MeasureReport individualMeasureReport) {
    logger.debug("Adding individual measure report: {}", individualMeasureReport.getId());

    // Set the reporter to the facility/org
    individualMeasureReport.setReporter(new Reference().setReference("Organization/" + this.org.getIdElement().getIdPart()));
    individualMeasureReport.getMeta().addProfile(config.isMHL() ?
            Constants.MHLIndividualMeasureReportProfileUrl : Constants.IndividualMeasureReportProfileUrl);

    bundle.addEntry().setResource(individualMeasureReport);

    // Identify line-level resources
    Map<IIdType, List<Reference>> lineLevelResources = getLineLevelResources(individualMeasureReport);

    // If reifying, retrieve the patient data bundle
    Bundle patientDataBundle = null;
    if (config.isReifyLineLevelResources()) {
      String individualMeasureReportId = individualMeasureReport.getIdElement().getIdPart();
      String patientDataBundleId = ReportIdHelper.getPatientDataBundleId(individualMeasureReportId);
      try {
        patientDataBundle = fhirDataProvider.getBundleById(patientDataBundleId);
      } catch (ResourceNotFoundException e) {
        logger.warn("Patient data bundle not found");
      }
    }

    // As specified in configuration, move/copy line-level resources
    for (Map.Entry<IIdType, List<Reference>> lineLevelResource : lineLevelResources.entrySet()) {
      IIdType resourceId = lineLevelResource.getKey();

      // Reify non-contained, non-patient line-level resources
      if (!resourceId.isLocal() && config.isReifyLineLevelResources()) {
        if (patientDataBundle == null) {
          continue;
        }
        Resource resource = patientDataBundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(_resource -> _resource.getResourceType() != ResourceType.Patient)
                .filter(_resource -> _resource.getIdElement().equals(resourceId))
                .findFirst()
                .orElse(null);
        if (resource == null) {
          continue;
        }
        addEntry(bundle, resource, false);
      }

      // Promote contained line-level resources
      if (resourceId.isLocal() && config.isPromoteLineLevelResources()) {
        Resource resource = individualMeasureReport.getContained().stream()
                .filter(_resource -> _resource.getIdElement().equals(resourceId))
                .findFirst()
                .orElse(null);
        if (resource == null) {
          continue;
        }
        individualMeasureReport.getContained().remove(resource);
        addEntry(bundle, resource, true);
        for (Reference reference : lineLevelResource.getValue()) {
          reference.setReference(getNonLocalId(resource));
        }
      }
    }
  }

  private Map<IIdType, List<Reference>> getLineLevelResources(MeasureReport individualMeasureReport) {
    Stream<Reference> evaluatedResources = individualMeasureReport.getEvaluatedResource().stream();
    Stream<Reference> supplementalDataReferences =
            individualMeasureReport.getExtension().stream()
                    .filter(extension -> SUPPLEMENTAL_DATA_EXTENSION_URLS.contains(extension.getUrl()))
                    .map(Extension::getValue)
                    .filter(value -> value instanceof Reference)
                    .map(value -> (Reference) value);
    return Stream.concat(supplementalDataReferences, evaluatedResources)
            .filter(reference -> reference.hasExtension(Constants.ExtensionCriteriaReference))
            .collect(Collectors.groupingBy(Reference::getReferenceElement, LinkedHashMap::new, Collectors.toList()));
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
