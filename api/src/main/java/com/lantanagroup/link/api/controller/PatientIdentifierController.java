package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.*;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.MeasureDefinition;
import com.lantanagroup.link.db.model.PatientId;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.db.model.tenant.EhrPatientList;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{tenantId}/poi")
public class PatientIdentifierController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(PatientIdentifierController.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private SharedService sharedService;

  @GetMapping
  public List<PatientList> searchPatientLists(@PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    return tenantService.getAllPatientLists()
            .stream().map(pl -> {
              pl.setPatients(null);
              return pl;
            })
            .collect(Collectors.toList());
  }

  @GetMapping("/{id}")
  public PatientList getPatientList(@PathVariable String id, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    return tenantService.getPatientList(id);
  }

  @PostMapping("/$query-list")
  public void queryPatientList(@PathVariable String tenantId) throws Exception {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null || tenantService.getConfig() == null) {
      return;
    } else if (tenantService.getConfig().getQueryList() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant is not configured to query the EHR for patient lists");
    }

    IGenericClient client = createClient(tenantService);
    List<EhrPatientList> filteredList = tenantService.getConfig().getQueryList().getLists();

    for (EhrPatientList patientListConfig : filteredList) {
      List<ListResource> sources = new ArrayList<>();
      for (String listId : patientListConfig.getListId()) {
        sources.add(this.readList(client, listId));
      }

      for (int j = 0; j < patientListConfig.getMeasureId().size(); j++) {
        PatientList patientList = this.convert(tenantService, sources, patientListConfig.getMeasureId().get(j));
        this.storePatientList(tenantService, patientList);
      }
    }
  }

  private IGenericClient createClient(TenantService tenantService) throws ClassNotFoundException {
    FhirContextProvider.getFhirContext().getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    IGenericClient client = FhirContextProvider.getFhirContext().newRestfulGenericClient(
            StringUtils.isNotEmpty(tenantService.getConfig().getQueryList().getFhirServerBase()) ?
                    tenantService.getConfig().getQueryList().getFhirServerBase() :
                    tenantService.getConfig().getFhirQuery().getFhirServerBase());

    client.registerInterceptor(new HapiFhirAuthenticationInterceptor(tenantService, this.applicationContext));
    return client;
  }

  private ListResource readList(IGenericClient client, String patientListId) {
    return client
            .read()
            .resource(ListResource.class)
            .withId(patientListId)
            .execute();
  }

  private PatientList convert(TenantService tenantService, List<ListResource> sources, String identifier) throws URISyntaxException {
    logger.info("Converting List resources to DB PatientLists");
    PatientList patientList = new PatientList();

    // TODO: Make ReportingPeriodMethods configurable
    ReportingPeriodCalculator calculator = new ReportingPeriodCalculator(ReportingPeriodMethods.CurrentMonth);

    patientList.setLastUpdated(new Date());
    patientList.setPeriodStart(calculator.getStart());
    patientList.setPeriodEnd(calculator.getEnd());
    patientList.setMeasureId(identifier);

    for (ListResource source : sources) {
      logger.info("Converting List: {}", source.getIdElement().getIdPart());
      for (ListResource.ListEntryComponent sourceEntry : source.getEntry()) {
        PatientId patientId = this.convertListItem(tenantService, sourceEntry);

        if (patientId != null && !patientList.getPatients().contains(patientId)) {
          patientList.getPatients().add(patientId);
        }
      }
    }

    return patientList;
  }

  private PatientId convertListItem(TenantService tenantService, ListResource.ListEntryComponent listEntry) throws URISyntaxException {
    URI baseUrl = new URI(tenantService.getConfig().getFhirQuery().getFhirServerBase());
    if (listEntry.getItem().hasReference()) {
      URI referenceUrl = new URI(listEntry.getItem().getReference());
      String reference;

      if (referenceUrl.isAbsolute()) {
        reference = baseUrl.relativize(referenceUrl).toString();
      } else {
        reference = listEntry.getItem().getReference();
      }

      return new PatientId(reference);
    } else if (listEntry.getItem().hasIdentifier()) {
      PatientId patientId = new PatientId();
      patientId.setIdentifier(listEntry.getItem().getIdentifier().getSystem() + "|" + listEntry.getItem().getIdentifier().getValue());
      return patientId;
    } else {
      logger.warn("List entry has does not have reference or identifier");
      return null;
    }
  }

  @PostMapping(value = "/fhir/List", consumes = {MediaType.APPLICATION_XML_VALUE})
  public void saveFhirList(
          @RequestBody() String body, @PathVariable String tenantId) throws Exception {
    logger.debug("Receiving patient identifier FHIR List in XML");

    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    ListResource list;

    try {
      list = this.ctx.newXmlParser().parseResource(ListResource.class, body);
    } catch (DataFormatException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FHIR XML cannot be parsed: " + ex.getMessage());
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FHIR XML cannot be parsed");
    }

    this.checkMeasureIdentifier(list);
    this.receiveFHIR(tenantService, list);
  }

  @PostMapping(value = "/fhir/List", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void getPatientIdentifierListJSON(
          @RequestBody() String body, @PathVariable String tenantId) throws Exception {
    logger.debug("Receiving patient identifier FHIR List in JSON");

    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    ListResource list;

    try {
      list = this.ctx.newJsonParser().parseResource(ListResource.class, body);
    } catch (DataFormatException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FHIR JSON cannot be parsed: " + ex.getMessage());
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FHIR JSON cannot be parsed");
    }

    checkMeasureIdentifier(list);
    this.receiveFHIR(tenantService, list);
  }

  private void checkMeasureIdentifier(ListResource list) {
    if (list.getIdentifier().size() < 1) {
      String msg = "Census list should have an identifier.";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
    Identifier measureIdentifier = list.getIdentifier().get(0);

    MeasureDefinition measureDefinition = this.sharedService.findMeasureDefinition(measureIdentifier.getValue());

    if (measureDefinition == null) {
      String msg = String.format("Measure %s (%s) not found on data store", measureIdentifier.getValue(), measureIdentifier.getSystem());
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }
  }

  private void storePatientList(TenantService tenantService, PatientList patientList) throws Exception {
    logger.info("Storing patient list");
    PatientList found = tenantService.findPatientList(patientList.getPeriodStart(), patientList.getPeriodEnd(), patientList.getMeasureId());

    // Merge the list of patients found with the new list
    if (found != null) {
      logger.info("Merging with pre-existing patient list that has {} (measure) {} (start) and {} (end)",
              patientList.getMeasureId(),
              patientList.getPeriodStart(),
              patientList.getPeriodEnd());
      patientList.setId(found.getId());
      found.merge(patientList);
    } else {
      found = patientList;
    }

    tenantService.savePatientList(found);
  }

  private void receiveFHIR(TenantService tenantService, ListResource listResource) throws Exception {
    List<Identifier> identifierList = listResource.getIdentifier();
    Extension applicablePeriodExt = listResource.getExtensionByUrl(Constants.ApplicablePeriodExtensionUrl);

    logger.info("Storing patient identifiers");

    if (identifierList.isEmpty()) {
      String msg = "Census List is missing identifier";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    // TODO: Call checkMeasureIdentifier here; remove the calls in getPatientIdentifierListXML/JSON

    if (applicablePeriodExt == null) {
      String msg = "Census list applicable-period extension is required";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    Period applicablePeriod = applicablePeriodExt.getValue().castToPeriod(applicablePeriodExt.getValue());

    if (applicablePeriod == null) {
      String msg = "applicable-period extension must have a value of type Period";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    if (!applicablePeriod.hasStart()) {
      String msg = "applicable-period.start must have start";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    if (!applicablePeriod.hasEnd()) {
      String msg = "applicable-period.start must have end";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    //system and value represents the measure intended for this patient id list
    String value = identifierList.get(0).getValue();
    DateTimeType startDate = applicablePeriod.getStartElement();
    DateTimeType endDate = applicablePeriod.getEndElement();
    String start = Helper.getFhirDate(LocalDateTime.of(startDate.getYear(), startDate.getMonth() + 1, startDate.getDay(), startDate.getHour(), startDate.getMinute(), startDate.getSecond()));
    String end = Helper.getFhirDate(LocalDateTime.of(endDate.getYear(), endDate.getMonth() + 1, endDate.getDay(), endDate.getHour(), endDate.getMinute(), endDate.getSecond()));

    PatientList patientList = new PatientList();
    patientList.setLastUpdated(new Date());
    patientList.setPeriodStart(start);
    patientList.setPeriodEnd(end);
    patientList.setMeasureId(value);

    for (ListResource.ListEntryComponent sourceEntry : listResource.getEntry()) {
      PatientId patientId = this.convertListItem(tenantService, sourceEntry);

      if (patientId != null) {
        patientList.getPatients().add(patientId);
      }
    }

    this.storePatientList(tenantService, patientList);
  }
}
