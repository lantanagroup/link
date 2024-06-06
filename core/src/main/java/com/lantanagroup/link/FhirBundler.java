package com.lantanagroup.link;

import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Aggregate;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.db.model.PatientMeasureReport;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.tenant.Bundling;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.MeasurePopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class FhirBundler {
  protected static final Logger logger = LoggerFactory.getLogger(FhirBundler.class);

  private final EventService eventService;

  private final TenantService tenantService;

  private Organization org;

  private Map<String, Resource> lineLevelResources;

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

  /**
   * Creates a Library resource that contains the query plan used for the report
   *
   * @param report The report
   * @return
   */
  private Library createQueryPlanLibrary(Report report) {
    String queryPlan = report.getQueryPlan();
    if (StringUtils.isEmpty(queryPlan)) {
      return null;
    }

    Library lib = new Library();
    lib.setId(UUID.randomUUID().toString());
    lib.setStatus(Enumerations.PublicationStatus.ACTIVE);
    lib.setType(new CodeableConcept().addCoding(new Coding()
            .setSystem(Constants.LibraryTypeSystem)
            .setCode(Constants.LibraryTypeModelDefinitionCode)));
    lib.setTitle("Link Query Plan");
    lib.addContent()
            .setContentType("text/yml")
            .setData(queryPlan.getBytes(StandardCharsets.UTF_8));

    return lib;
  }

  public Bundle generateBundle(Collection<Aggregate> aggregates, Report report) {
    Bundle bundle = this.createBundle();
    bundle.addEntry().setResource(this.getOrg());
    Device device = report.getDeviceInfo();
    if (device != null) {
      bundle.addEntry().setResource(device);
    }
    Library library = this.createQueryPlanLibrary(report);
    if (library != null) {
      bundle.addEntry().setResource(library);
    }
    this.lineLevelResources = new HashMap<>();

    triggerEvent(this.tenantService, EventTypes.BeforeBundling, bundle);

    if (this.getBundlingConfig().isIncludeCensuses()) {
      this.addCensuses(bundle, report);
    }

    for (Aggregate aggregate : aggregates) {
      this.addAggregateMeasureReport(bundle, aggregate.getReport());
    }

    Map<String, List<String>> pmrIdsByHashedPatientId = new HashMap<>();
    for (Aggregate aggregate : aggregates) {
      List<String> pmrIds = getPatientMeasureReportIds(aggregate);
      if (pmrIds == null) {
        continue;
      }
      for (String pmrId : pmrIds) {
        String hashedPatientId = ReportIdHelper.getHashedPatientId(pmrId);
        pmrIdsByHashedPatientId.computeIfAbsent(hashedPatientId, k -> new ArrayList<>()).add(pmrId);
      }
    }

    for (List<String> pmrIds : pmrIdsByHashedPatientId.values()) {
      for (String pmrId : pmrIds) {
        PatientMeasureReport patientMeasureReport = this.tenantService.getPatientMeasureReport(pmrId);
        if (patientMeasureReport == null) {
          logger.warn("Patient measure report not found in database: {}", pmrId);
          continue;
        }
        MeasureReport individualMeasureReport = patientMeasureReport.getMeasureReport();
        individualMeasureReport.getContained().forEach(this::cleanupResource);  // Ensure all contained resources have the right profiles
        this.addIndividualMeasureReport(bundle, individualMeasureReport);
      }
    }

    triggerEvent(this.tenantService, EventTypes.AfterBundling, bundle);

    this.cleanEntries(bundle);

    long noProfileResources = bundle.getEntry().stream()
            .filter(e -> e.getResource().getMeta().getProfile().isEmpty())
            .count();

    if (noProfileResources > 0) {
      logger.warn("{} resources in the bundle don't have profiles", noProfileResources);
    }

    // Sort the entries so they're always in the same order
    FhirBundlerEntrySorter.sort(bundle);

    return bundle;
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
            .setValue("urn:uuid:" + UUID.randomUUID());
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
    if (resource instanceof DomainResource) {
      DomainResource domainResource = (DomainResource) resource;

      // Remove extensions from resources
      domainResource.getExtension().removeIf(e -> e.getUrl() != null && REMOVE_EXTENSIONS.contains(e.getUrl()));

      // Remove extensions from group/populations of MeasureReports
      if (resource instanceof MeasureReport) {
        MeasureReport measureReport = (MeasureReport) resource;
        measureReport.getGroup().forEach(g -> {
          g.getPopulation().forEach(p -> {
            p.getExtension().removeIf(e -> e.getUrl() != null && REMOVE_EXTENSIONS.contains(e.getUrl()));
          });
        });

        measureReport.getEvaluatedResource().forEach(er -> {
          er.getExtension().removeIf(e -> e.getUrl() != null && REMOVE_EXTENSIONS.contains(e.getUrl()));
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

  private List<String> getPatientMeasureReportIds(Aggregate aggregate) {
    MeasureReport aggregateMeasureReport = aggregate.getReport();
    String subjectListId = null;

    for (MeasureReport.MeasureReportGroupComponent group : aggregateMeasureReport.getGroup()) {
      // Find the initial-population
      MeasureReport.MeasureReportGroupPopulationComponent population = group.getPopulation().stream()
              .filter(p -> p.getCode() != null && p.getCode().getCodingFirstRep() != null && p.getCode().getCodingFirstRep().getCode() != null && p.getCode().getCodingFirstRep().getCode().equals(MeasurePopulation.INITIALPOPULATION.toCode()))
              .findFirst()
              .orElse(null);

      // Make sure there is a refefence to a contained subject list
      if (population != null && population.hasSubjectResults() && !population.getSubjectResults().getReference().contains("/")) {
        subjectListId = population.getSubjectResults().getReference().replace("#", "");
      }
    }

    if (subjectListId == null) {
      logger.warn("No subject list for initial-population on aggregate measure report {}", aggregateMeasureReport.getIdElement().getIdPart());
      return null;
    }

    String finalSubjectListId = subjectListId;
    ListResource subjectList = aggregateMeasureReport.getContained().stream()
            .filter(c -> c.getResourceType() == ResourceType.List && c.getIdElement().getIdPart().replace("#", "").equals(finalSubjectListId))
            .map(c -> (ListResource) c)
            .findFirst()
            .orElse(null);

    if (subjectList == null) {
      logger.error("Aggregate measure report {} does not have a contained subject list", aggregateMeasureReport.getIdElement().getIdPart());
      return null;
    }

    List<String> patientMeasureReportIds = new ArrayList<>();
    for (ListResource.ListEntryComponent subject : subjectList.getEntry()) {
      String patientMeasureReportId = subject.getItem().getReferenceElement().getIdPart();
      if (patientMeasureReportId == null) {
        logger.warn("Found null ID in subject list");
        continue;
      }
      patientMeasureReportIds.add(patientMeasureReportId);
    }

    return patientMeasureReportIds;
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
        String lineLevelResourceId = getNonLocalId(contained);
        Resource found = this.lineLevelResources.get(lineLevelResourceId);

        if (found == null) {
          bundle.addEntry().setResource(contained);
          this.lineLevelResources.put(lineLevelResourceId, contained);
        } else {
          Resource foundClone = found.copy().setMeta(null);
          Resource containedClone = contained.copy().setMeta(null);
          if (!foundClone.equalsDeep(containedClone)) {
            logger.warn("Previously promoted resource {} is not equivalent", lineLevelResourceId);
          }
          for (CanonicalType profile : contained.getMeta().getProfile()) {
            String value = profile.getValue();
            if (!found.getMeta().hasProfile(value)) {
              logger.debug("Adding profile {} to previously promoted resource {}", value, lineLevelResourceId);
              found.getMeta().addProfile(value);
            }
          }
        }

        String oldReference = "#" + getIdPart(contained);
        String newReference = lineLevelResourceId;
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
