package com.lantanagroup.link;

import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.db.model.PatientMeasureReport;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.tenant.Bundling;
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

  private Organization org;

  private final List<String> REMOVE_EXTENSIONS = List.of(
          "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.population.description",
          "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.supplementalDataElement.reference",
          "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-criteriaReference",
          "http://open.epic.com/FHIR/StructureDefinition/extension/accidentrelated",
          "http://open.epic.com/FHIR/StructureDefinition/extension/billing-organization",
          "http://open.epic.com/FHIR/StructureDefinition/extension/epic-id",
          "http://open.epic.com/FHIR/StructureDefinition/extension/ip-admit-datetime",
          "http://open.epic.com/FHIR/StructureDefinition/extension/observation-datetime",
          "http://open.epic.com/FHIR/StructureDefinition/extension/specialty",
          "http://open.epic.com/FHIR/StructureDefinition/extension/team-name",
          "https://open.epic.com/FHIR/StructureDefinition/extension/patient-merge-unmerge-instant"
  );

  public FhirBundler(EventService eventService, TenantService tenantService) {
    this.eventService = eventService;
    this.tenantService = tenantService;
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

  public Bundle generateBundle(Collection<MeasureReport> aggregateMeasureReports, Report report) {
    Bundle bundle = this.createBundle();
    bundle.addEntry().setResource(this.getOrg());

    triggerEvent(this.tenantService, EventTypes.BeforeBundling, bundle);

    if (this.getBundlingConfig().isIncludeCensuses()) {
      this.addCensuses(bundle, report);
    }

    for (MeasureReport aggregateMeasureReport : aggregateMeasureReports) {
      this.addMeasureReports(bundle, aggregateMeasureReport);
    }

    triggerEvent(this.tenantService, EventTypes.AfterBundling, bundle);

    this.cleanEntries(bundle);

    return bundle;
  }

  private Organization createOrganization() {
    Organization org = new Organization();
    org.getMeta().addProfile(Constants.SubmittingOrganizationProfile);
    org.setActive(true);

    if (!StringUtils.isEmpty(this.getBundlingConfig().getNpi())) {
      org.setId(String.format("%s", this.getBundlingConfig().getNpi().hashCode()));
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
    resource.setMeta(null);

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

      // Only allow meta.profile
      if (resource.getMeta() != null) {
        Meta cleanedMeta = new Meta();
        cleanedMeta.setProfile(resource.getMeta().getProfile());
        resource.setMeta(cleanedMeta);
      }

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
    List<PatientList> patientLists = this.tenantService.getPatientLists(report.getPatientLists());

    return patientLists.stream().map(pl -> {
      ListResource listResource = new ListResource();
      listResource.setId(pl.getId());
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
          String[] identifierSplit = pid.getIdentifier().split("|");
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

  private void addMeasureReports(Bundle bundle, MeasureReport aggregateMeasureReport) {
    logger.debug("Adding measure reports: {}", aggregateMeasureReport.getMeasure());

    this.addAggregateMeasureReport(bundle, aggregateMeasureReport);

    if (!this.getBundlingConfig().isIncludeIndividualMeasureReports()) {
      return;
    }

    List<String> individualMeasureReportIds = new ArrayList<>();
    aggregateMeasureReport.getContained()
            .stream().filter(c -> c.getResourceType() == ResourceType.List)
            .forEach(c -> {
              ListResource listResource = (ListResource) c;
              listResource.getEntry().forEach(e -> {
                String[] referenceSplit = e.getItem().getReference().split("/");
                individualMeasureReportIds.add(referenceSplit[1]);
              });
            });

    List<PatientMeasureReport> individualMeasureReports =
            this.tenantService.getPatientMeasureReports(individualMeasureReportIds);

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
  }

  private String getNonLocalId(IBaseResource resource) {
    return String.format("%s/%s", resource.fhirType(), getIdPart(resource));
  }

  private String getIdPart(IBaseResource resource) {
    return resource.getIdElement().getIdPart().replaceAll("^#", "");
  }
}
