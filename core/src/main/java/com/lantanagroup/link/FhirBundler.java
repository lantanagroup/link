package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.ConceptMap;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Bundling;
import com.lantanagroup.link.model.ApiInfoModel;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FhirBundler {
  protected static final Logger logger = LoggerFactory.getLogger(FhirBundler.class);
  private static final List<String> SUPPLEMENTAL_DATA_EXTENSION_URLS = List.of(
          "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData",
          "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.supplementalDataElement.reference");

  private final EventService eventService;

  private final TenantService tenantService;
  private final ApiConfig apiConfig;

  private Organization org;

  private Device device;

  private final List<String> REMOVE_EXTENSIONS = List.of(
          "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.population.description",
          "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.supplementalDataElement.reference",
          "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-criteriaReference",
          "http://open.epic.com/FHIR/StructureDefinition/extension/accidentrelated",
          "http://open.epic.com/FHIR/StructureDefinition/extension/epic-id",
          "http://open.epic.com/FHIR/StructureDefinition/extension/ip-admit-datetime",
          "http://open.epic.com/FHIR/StructureDefinition/extension/observation-datetime",
          "http://open.epic.com/FHIR/StructureDefinition/extension/specialty",
          "http://open.epic.com/FHIR/StructureDefinition/extension/team-name",
          "https://open.epic.com/FHIR/StructureDefinition/extension/patient-merge-unmerge-instant"
  );

  public FhirBundler(EventService eventService, TenantService tenantService, ApiConfig apiConfig) {
    this.eventService = eventService;
    this.tenantService = tenantService;
    this.apiConfig = apiConfig;
  }

  private Bundling getBundlingConfig() {
    return this.tenantService.getConfig().getBundling() != null ?
            this.tenantService.getConfig().getBundling() :
            new Bundling();
  }

  private Organization getOrg() {
    if (this.org == null) {
      this.org = this.createOrganization();
    }

    return this.org;
  }

  private static void addPropertyToDevice(Device device, String category, List<String> events) {
    if (events == null) {
      return;
    }

    for (String event : events) {
      Device.DevicePropertyComponent property = device.addProperty();
      property.getType().addCoding().setCode("event");

      String theEvent = event.indexOf(".") > 0 ? event.substring(event.lastIndexOf(".") + 1) : event;
      property.addValueCode().addCoding().setCode(category + "-" + theEvent);
    }
  }

  private Device getDevice() {
    if (this.device == null) {
      this.device = this.createDevice();
    }

    return this.device;
  }

  public Bundle generateBundle(Collection<Aggregate> aggregates, Report report) {
    Bundle bundle = this.createBundle();
    bundle.addEntry().setResource(this.getOrg());
    bundle.addEntry().setResource(this.getDevice());

    triggerEvent(this.tenantService, EventTypes.BeforeBundling, bundle);

    if (this.getBundlingConfig().isIncludeCensuses()) {
      this.addCensuses(bundle, report);
    }

    for (Aggregate aggregate : aggregates) {
      this.addMeasureReports(bundle, aggregate);
    }

    triggerEvent(this.tenantService, EventTypes.AfterBundling, bundle);

    this.cleanEntries(bundle);

    return bundle;
  }

  private Device createDevice() {
    ApiInfoModel apiInfoModel = Helper.getVersionInfo(this.apiConfig.getEvaluationService());
    Device device = new Device();
    device.setId(UUID.randomUUID().toString());
    device.getMeta().addProfile(Constants.SubmittingDeviceProfile);
    device.addDeviceName().setName(this.apiConfig.getName());

    if (StringUtils.isNotEmpty(apiInfoModel.getVersion())) {
      device.addVersion()
              .setType(new CodeableConcept().addCoding(new Coding().setCode("version")))
              .setComponent(new Identifier().setValue("api"))
              .setValue(apiInfoModel.getVersion());
    }

    if (StringUtils.isNotEmpty(apiInfoModel.getBuild())) {
      device.addVersion()
              .setType(new CodeableConcept().addCoding(new Coding().setCode("build")))
              .setComponent(new Identifier().setValue("api"))
              .setValue(apiInfoModel.getBuild());
    }

    if (StringUtils.isNotEmpty(apiInfoModel.getCommit())) {
      device.addVersion()
              .setType(new CodeableConcept().addCoding(new Coding().setCode("commit")))
              .setComponent(new Identifier().setValue("api"))
              .setValue(apiInfoModel.getCommit());
    }

    if (StringUtils.isNotEmpty(apiInfoModel.getCqfVersion())) {
      device.addVersion()
              .setType(new CodeableConcept().addCoding(new Coding().setCode("version")))
              .setComponent(new Identifier().setValue("cqf-ruler"))
              .setValue(apiInfoModel.getCqfVersion());
    }

    addPropertyToDevice(device, "BeforeMeasureResolution", this.tenantService.getConfig().getEvents().getBeforeMeasureResolution());
    addPropertyToDevice(device, "AfterMeasureResolution", this.tenantService.getConfig().getEvents().getAfterMeasureResolution());
    addPropertyToDevice(device, "OnRegeneration", this.tenantService.getConfig().getEvents().getOnRegeneration());
    addPropertyToDevice(device, "BeforePatientOfInterestLookup", this.tenantService.getConfig().getEvents().getBeforePatientOfInterestLookup());
    addPropertyToDevice(device, "AfterPatientOfInterestLookup", this.tenantService.getConfig().getEvents().getAfterPatientOfInterestLookup());
    addPropertyToDevice(device, "BeforePatientDataQuery", this.tenantService.getConfig().getEvents().getBeforePatientDataQuery());
    addPropertyToDevice(device, "AfterPatientResourceQuery", this.tenantService.getConfig().getEvents().getAfterPatientResourceQuery());
    addPropertyToDevice(device, "AfterPatientDataQuery", this.tenantService.getConfig().getEvents().getAfterPatientDataQuery());
    addPropertyToDevice(device, "AfterApplyConceptMaps", this.tenantService.getConfig().getEvents().getAfterApplyConceptMaps());
    addPropertyToDevice(device, "BeforePatientDataStore", this.tenantService.getConfig().getEvents().getBeforePatientDataStore());
    addPropertyToDevice(device, "AfterPatientDataStore", this.tenantService.getConfig().getEvents().getAfterPatientDataStore());
    addPropertyToDevice(device, "BeforeMeasureEval", this.tenantService.getConfig().getEvents().getBeforeMeasureEval());
    addPropertyToDevice(device, "AfterMeasureEval", this.tenantService.getConfig().getEvents().getAfterMeasureEval());
    addPropertyToDevice(device, "BeforeReportStore", this.tenantService.getConfig().getEvents().getBeforeReportStore());
    addPropertyToDevice(device, "AfterReportStore", this.tenantService.getConfig().getEvents().getAfterReportStore());
    addPropertyToDevice(device, "BeforeBundling", this.tenantService.getConfig().getEvents().getBeforeBundling());
    addPropertyToDevice(device, "AfterBundling", this.tenantService.getConfig().getEvents().getAfterBundling());

    List<ConceptMap> conceptMaps = this.tenantService.getAllConceptMaps();

    if (!conceptMaps.isEmpty()) {
      Device.DevicePropertyComponent property = device.addProperty();
      property.setType(new CodeableConcept().addCoding(new Coding().setCode("concept-map")));

      for (ConceptMap conceptMap : this.tenantService.getAllConceptMaps()) {
        property.addValueCode(new CodeableConcept().addCoding(new Coding().setCode(conceptMap.getId())).setText(conceptMap.getConceptMap().getName()));
      }
    }

    return device;
  }

  private Organization createOrganization() {
    Organization org = new Organization();
    org.getMeta().addProfile(Constants.SubmittingOrganizationProfile);
    org.setActive(true);

    if (!StringUtils.isEmpty(this.getBundlingConfig().getNpi())) {
      org.setId(DigestUtils.sha1Hex(this.getBundlingConfig().getNpi()));
    } else {
      org.setId(UUID.randomUUID().toString());
    }

    org.addType()
            .addCoding()
            .setSystem(Constants.OrganizationTypeSystem)
            .setCode("prov")
            .setDisplay("Healthcare Provider");

    if (!StringUtils.isEmpty(this.getBundlingConfig().getName())) {
      org.setName(this.getBundlingConfig().getName());
    }

    if (!StringUtils.isEmpty(this.getBundlingConfig().getNpi())) {
      org.addIdentifier()
              .setSystem(Constants.NationalProviderIdentifierSystemUrl)
              .setValue(this.getBundlingConfig().getNpi());
    }

    if (StringUtils.isNotEmpty(this.tenantService.getConfig().getCdcOrgId())) {
      org.addIdentifier()
              .setSystem(Constants.CdcOrgIdSystem)
              .setValue(this.tenantService.getConfig().getCdcOrgId());
    }

    if (!StringUtils.isEmpty(this.getBundlingConfig().getPhone())) {
      org.addTelecom()
              .setSystem(ContactPoint.ContactPointSystem.PHONE)
              .setValue(this.getBundlingConfig().getPhone());
    }

    if (!StringUtils.isEmpty(this.getBundlingConfig().getEmail())) {
      org.addTelecom()
              .setSystem(ContactPoint.ContactPointSystem.EMAIL)
              .setValue(this.getBundlingConfig().getEmail());
    }

    if (StringUtils.isEmpty(this.getBundlingConfig().getPhone()) && StringUtils.isEmpty(this.getBundlingConfig().getEmail())) {
      org.addTelecom()
              .addExtension()
              .setUrl(Constants.DataAbsentReasonExtensionUrl)
              .setValue(new CodeType(Constants.DataAbsentReasonUnknownCode));
    }

    if (this.getBundlingConfig().getAddress() != null) {
      org.addAddress(FhirHelper.getFHIRAddress(this.getBundlingConfig().getAddress()));
    } else {
      org.addAddress().addExtension()
              .setUrl(Constants.DataAbsentReasonExtensionUrl)
              .setValue(new CodeType().setValue(Constants.DataAbsentReasonUnknownCode));
    }

    return org;
  }

  private Bundle createBundle() {
    Bundle bundle = new Bundle();
    bundle.getMeta()
            .addProfile(Constants.ReportBundleProfileUrl)
            .addTag(Constants.MainSystem, "report", "Report");
    bundle.getIdentifier()
            .setSystem(Constants.IdentifierSystem)
            .setValue("urn:uuid:" + UUID.randomUUID().toString());
    bundle.setType(this.getBundlingConfig().getBundleType());
    bundle.setTimestamp(new Date());
    return bundle;
  }

  private void triggerEvent(TenantService tenantService, EventTypes eventType, Bundle bundle) {
    if (eventService == null) {
      return;
    }

    try {
      eventService.triggerDataEvent(tenantService, eventType, bundle, null, null, null);
    } catch (Exception e) {
      logger.error(String.format("Error occurred in %s handler", eventType), e);
    }
  }

  /**
   * Add profiles to the resource if they are clinical resources
   * Remove other meta details
   * Remove Epic extensions
   * Remove CQF-Ruler extensions
   *
   * @param resource
   */
  private void cleanupResource(Resource resource) {
    resource.getMeta().getProfile().clear();

    List<String> profiles = null;

    switch (resource.getResourceType()) {
      case Patient:
        profiles = List.of(Constants.QiCorePatientProfileUrl, Constants.UsCorePatientProfileUrl);
        break;
      case Encounter:
        profiles = List.of(Constants.UsCoreEncounterProfileUrl);
        break;
      case MedicationRequest:
        profiles = List.of(Constants.UsCoreMedicationRequestProfileUrl);
        break;
      case Medication:
        profiles = List.of(Constants.UsCoreMedicationProfileUrl);
        break;
      case Condition:
        profiles = List.of(Constants.UsCoreConditionProfileUrl);
        break;
      case Observation:
        profiles = List.of(Constants.UsCoreObservationProfileUrl);
        break;
    }

    if (profiles != null) {
      profiles.forEach(profile -> {
        if (resource.getMeta().getProfile().stream().noneMatch(p -> p.getValue().equals(profile))) {   // Don't duplicate profile if it already exists
          resource.getMeta().addProfile(profile);
        }
      });
    }

    if (resource instanceof DomainResource) {
      DomainResource domainResource = (DomainResource) resource;

      // Remove extensions from resources
      domainResource.getExtension().stream()
              .filter(e -> e.getUrl() != null && REMOVE_EXTENSIONS.contains(e.getUrl()))
              .collect(Collectors.toList())
              .forEach(re -> domainResource.getExtension().remove(re));

      // Remove extensions from group/populations of MeasureReports
      if (resource instanceof MeasureReport) {
        MeasureReport measureReport = (MeasureReport) resource;
        measureReport.getGroup().forEach(g -> {
          g.getPopulation().forEach(p -> {
            p.getExtension().stream()
                    .filter(e -> e.getUrl() != null && REMOVE_EXTENSIONS.contains(e.getUrl()))
                    .collect(Collectors.toList())
                    .forEach(re -> p.getExtension().remove(re));
          });
        });

        measureReport.getEvaluatedResource().forEach(er -> {
          er.getExtension().stream()
                  .filter(e -> e.getUrl() != null && REMOVE_EXTENSIONS.contains(e.getUrl()))
                  .collect(Collectors.toList())
                  .forEach(re -> er.getExtension().remove(re));
        });
      }
    }
  }

  private void cleanEntries(Bundle bundle) {
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      Resource resource = entry.getResource();
      String resourceId = getNonLocalId(resource);
      resource.setId(resourceId);

      if (resource instanceof DomainResource) {
        ((DomainResource) resource).setText(null);
      }
      entry.setFullUrl(String.format(Constants.BundlingFullUrlFormat, resourceId));
      if (this.getBundlingConfig().getBundleType() == Bundle.BundleType.TRANSACTION
              || this.getBundlingConfig().getBundleType() == Bundle.BundleType.BATCH) {
        entry.getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(resourceId);
      }
    }
  }

  public List<ListResource> getPatientLists(Report report) {
    List<PatientList> patientLists = this.tenantService.getPatientLists(report.getId());

    return patientLists.stream().map(pl -> {
      ListResource listResource = new ListResource();
      listResource.setId(pl.getId().toString());
      listResource.addExtension()
              .setUrl(Constants.ApplicablePeriodExtensionUrl)
              .setValue(new Period()
                      .setStartElement(new DateTimeType(pl.getPeriodStart()))
                      .setEndElement(new DateTimeType(pl.getPeriodEnd())));
      listResource.addIdentifier()
              .setSystem(Constants.MainSystem)
              .setValue(pl.getMeasureId());

      listResource.setEntry(pl.getPatients().stream().map(pid -> {
        ListResource.ListEntryComponent entry = new ListResource.ListEntryComponent();

        if (StringUtils.isNotEmpty(pid.getIdentifier())) {
          String[] identifierSplit = pid.getIdentifier().split("\\|");
          Identifier identifier = new Identifier();
          entry.getItem().setIdentifier(identifier);

          if (identifierSplit.length == 2) {
            identifier.setSystem(identifierSplit[0]);
            identifier.setValue(identifierSplit[1]);
          } else if (identifierSplit.length == 1) {
            identifier.setValue(identifierSplit[0]);
          } else {
            logger.error("Expected one or two parts to the identifier, but got {}", identifierSplit.length);
          }
        } else if (StringUtils.isNotEmpty(pid.getReference())) {
          entry.getItem().setReference(pid.getReference());
        }

        return entry;
      }).collect(Collectors.toList()));

      return listResource;
    }).collect(Collectors.toList());
  }

  private void setCensusProperties(ListResource census) {
    census.setMeta(new Meta());
    census.getMeta().addProfile(Constants.CensusProfileUrl);
    census.setMode(ListResource.ListMode.SNAPSHOT);
    census.setStatus(ListResource.ListStatus.CURRENT);
  }

  private void addCensuses(Bundle bundle, Report report) {
    logger.debug("Adding censuses");
    Collection<ListResource> patientLists = this.getPatientLists(report);

    if (patientLists.isEmpty()) {
      return;
    }

    if (patientLists.size() > 1 && this.getBundlingConfig().isMergeCensuses()) {
      logger.debug("Merging censuses");
      ListResource mergedCensus = patientLists.iterator().next().copy();
      mergedCensus.setId(UUID.randomUUID().toString());
      mergedCensus.getEntry().clear();
      this.setCensusProperties(mergedCensus);
      for (ListResource census : patientLists) {
        FhirHelper.mergePatientLists(mergedCensus, census);
      }
      bundle.addEntry().setResource(mergedCensus);
    } else {
      for (ListResource census : patientLists) {
        logger.debug("Adding census: {}", census.getId());
        this.setCensusProperties(census);
        bundle.addEntry().setResource(census);
      }
    }
  }

  private void addMeasureReports(Bundle bundle, Aggregate aggregate) {
    MeasureReport aggregateMeasureReport = aggregate.getReport();
    logger.debug("Adding measure reports: {}", aggregateMeasureReport.getMeasure());

    this.addAggregateMeasureReport(bundle, aggregateMeasureReport);

    List<PatientMeasureReport> individualMeasureReports =
            this.tenantService.getPatientMeasureReports(aggregate.getReportId(), aggregate.getMeasureId());

    for (PatientMeasureReport patientMeasureReport : individualMeasureReports) {
      MeasureReport individualMeasureReport = patientMeasureReport.getMeasureReport();
      individualMeasureReport.getContained().forEach(this::cleanupResource);  // Ensure all contained resources have the right profiles
      this.addIndividualMeasureReport(bundle, individualMeasureReport);
    }
  }

  private void addAggregateMeasureReport(Bundle bundle, MeasureReport aggregateMeasureReport) {
    logger.debug("Adding aggregate measure report: {}", aggregateMeasureReport.getId());

    aggregateMeasureReport.getMeta().addProfile(Constants.SubjectListMeasureReportProfile);

    // Set the reporter to the facility/org
    aggregateMeasureReport.setReporter(new Reference().setReference("Organization/" + this.getOrg().getIdElement().getIdPart()));

    bundle.addEntry().setResource(aggregateMeasureReport);
  }

  private void addIndividualMeasureReport(Bundle bundle, MeasureReport individualMeasureReport) {
    logger.debug("Adding individual measure report: {}", individualMeasureReport.getId());

    this.cleanupResource(individualMeasureReport);

    // Set the reporter to the facility/org
    individualMeasureReport.setReporter(new Reference().setReference("Organization/" + this.getOrg().getIdElement().getIdPart()));
    individualMeasureReport.getMeta().addProfile(Constants.IndividualMeasureReportProfileUrl);

    // Clean up the contained resources within the measure report
    individualMeasureReport.getContained().stream()
            .filter(c -> c.hasId() && c.getIdElement().getIdPart().startsWith("#LCR-"))
            .forEach(c -> {
              // Remove the LCR- prefix added by CQL
              c.setId(c.getIdElement().getIdPart().substring(5));

              // Update references to the evaluated resource to point to the contained reference (for validation purposes)
              individualMeasureReport.getEvaluatedResource().stream()
                      .filter(er -> er.hasReference() && er.getReference().equals(c.getResourceType().toString() + "/" + c.getIdElement().getIdPart()))
                      .forEach(er -> er.setReference("#" + c.getIdElement().getIdPart()));
            });

    bundle.addEntry().setResource(individualMeasureReport);

    if (this.getBundlingConfig().isPromoteLineLevelResources()) {
      List<Reference> references = FhirScanner.findReferences(individualMeasureReport);

      for (int i = 0; i < individualMeasureReport.getContained().size(); i++) {
        Resource contained = individualMeasureReport.getContained().get(i);
        String containedId = contained.getIdElement().getIdPart().replace("#", "");
        Optional<Bundle.BundleEntryComponent> found = bundle.getEntry().stream()
                .filter(e ->
                        e.getResource().getResourceType() == contained.getResourceType() &&
                                e.getResource().getIdElement().getIdPart().equals(contained.getIdElement().getIdPart()))
                .findFirst();

        if (found.isEmpty()) {
          bundle.addEntry().setResource(contained);
        } else {
          if (!found.get().getResource().equalsDeep(contained)) {
            logger.error("Need to change the id of {}/{} because another resource has already been promoted with the same ID that is not the same", contained.getResourceType(), contained.getIdElement().getIdPart());
          } else {
            logger.debug("Resource {}/{} already has a copy that has been promoted. Not promoting/replacing.", contained.getResourceType(), contained.getIdElement().getIdPart());
          }
        }

        String oldReference = "#" + containedId;
        String newReference = contained.getResourceType() + "/" + containedId;
        references.stream()
                .filter(r -> r.getReference() != null && r.getReference().equals(oldReference))
                .forEach(r -> r.setReference(newReference));
      }

      individualMeasureReport.getContained().clear();
    }
  }

  private String getNonLocalId(IBaseResource resource) {
    String id = getIdPart(resource);

    if (StringUtils.isEmpty(id)) {
      return null;
    }

    return String.format("%s/%s", resource.fhirType(), id);
  }

  private String getIdPart(IBaseResource resource) {
    if (resource.getIdElement() == null || resource.getIdElement().getIdPart() == null) {
      return null;
    }

    return resource.getIdElement().getIdPart().replaceAll("^#", "");
  }
}
