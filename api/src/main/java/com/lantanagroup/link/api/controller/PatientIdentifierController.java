package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.*;
import com.lantanagroup.link.api.scheduling.ReportingPeriodCalculator;
import com.lantanagroup.link.config.QueryListConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.config.scheduling.ReportingPeriodMethods;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reportability Response Controller
 */
@RestController
@RequestMapping("/api/poi")
public class PatientIdentifierController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(PatientIdentifierController.class);

  private final FhirContext fhirContext = FhirContextProvider.getFhirContext();
  @Autowired
  private USCoreConfig usCoreConfig;
  @Autowired
  private QueryConfig queryConfig;
  @Autowired
  private ApplicationContext applicationContext;
  @Autowired
  private QueryListConfig queryListConfig;

  @PostMapping("/$query-list")
  public void queryPatientList() throws Exception {
    List<QueryListConfig.PatientList> filteredList = this.queryListConfig.getLists();

    for (QueryListConfig.PatientList listResource : filteredList) {
      ListResource source = this.readList(listResource.getListId());

      for (int j = 0; j < listResource.getCensusIdentifier().size(); j++) {
        ListResource target = this.transformList(source, listResource.getCensusIdentifier().get(j));

        this.receiveFHIR(target);
      }
    }
  }

  private ListResource readList(String patientListId) throws ClassNotFoundException {
    this.fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    IGenericClient client = this.fhirContext.newRestfulGenericClient(
            StringUtils.isNotEmpty(this.queryListConfig.getFhirServerBase()) ?
                    this.queryListConfig.getFhirServerBase() :
                    this.usCoreConfig.getFhirServerBase());

    client.registerInterceptor(new HapiFhirAuthenticationInterceptor(this.queryConfig, this.applicationContext));
    return client.fetchResourceFromUrl(ListResource.class, patientListId);
  }

  private ListResource transformList(ListResource source, String censusIdentifier) throws URISyntaxException {
    logger.info("Transforming");
    ListResource target = new ListResource();
    Period period = new Period();

    // TODO: Make ReportingPeriodMethods configurable
    ReportingPeriodCalculator calculator = new ReportingPeriodCalculator(ReportingPeriodMethods.CurrentMonth);
    period.setStartElement(new DateTimeType(calculator.getStart()));
    period.setEndElement(new DateTimeType(calculator.getEnd()));

    target.addExtension(Constants.ApplicablePeriodExtensionUrl, period);
    target.addIdentifier()
            .setSystem(Constants.MainSystem)
            .setValue(censusIdentifier);
    target.setStatus(ListResource.ListStatus.CURRENT);
    target.setMode(ListResource.ListMode.WORKING);
    target.setTitle(String.format("Census List for %s", censusIdentifier));
    target.setCode(source.getCode());
    target.setDate(source.getDate());
    URI baseUrl = new URI(usCoreConfig.getFhirServerBase());
    for (ListResource.ListEntryComponent sourceEntry : source.getEntry()) {
      target.addEntry(transformListEntry(sourceEntry, baseUrl));
    }
    return target;
  }

  private ListResource.ListEntryComponent transformListEntry(ListResource.ListEntryComponent source, URI baseUrl)
          throws URISyntaxException {
    ListResource.ListEntryComponent target = source.copy();
    if (target.getItem().hasReference()) {
      URI referenceUrl = new URI(target.getItem().getReference());
      if (referenceUrl.isAbsolute()) {
        target.getItem().setReference(baseUrl.relativize(referenceUrl).toString());
      }
    }
    return target;
  }

  @PostMapping(value = "/fhir/List", consumes = {MediaType.APPLICATION_XML_VALUE})
  public void getPatientIdentifierListXML(
          @RequestBody() String body) throws Exception {
    logger.debug("Receiving patient identifier FHIR List in XML");

    ListResource list = this.ctx.newXmlParser().parseResource(ListResource.class, body);
    checkMeasureIdentifier(list);
    this.receiveFHIR(list);
  }

  @PostMapping(value = "/fhir/List", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void getPatientIdentifierListJSON(
          @RequestBody() String body) throws Exception {
    logger.debug("Receiving patient identifier FHIR List in JSON");

    ListResource list = this.ctx.newJsonParser().parseResource(ListResource.class, body);
    checkMeasureIdentifier(list);
    this.receiveFHIR(list);
  }

  private void checkMeasureIdentifier(ListResource list) {
    if (list.getIdentifier().size() < 1) {
      String msg = "Census list should have an identifier.";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
    Identifier measureIdentifier = list.getIdentifier().get(0);
    FhirDataProvider evaluationDataProvider = new FhirDataProvider(this.config.getEvaluationService());
    Measure measure = evaluationDataProvider.findMeasureByIdentifier(measureIdentifier);
    if (measure == null) {
      String msg = String.format("Measure %s (%s) not found on data store", measureIdentifier.getValue(), measureIdentifier.getSystem());
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }
  }

  private void receiveFHIR(Resource resource) throws Exception {
    logger.info("Storing patient identifiers");
    resource.setId((String) null);

    if (resource instanceof ListResource) {
      ListResource list = (ListResource) resource;

      List<Identifier> identifierList = list.getIdentifier();

      if (identifierList.isEmpty()) {
        String msg = "Census List is missing identifier";
        logger.error(msg);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
      }

      // TODO: Call checkMeasureIdentifier here; remove the calls in getPatientIdentifierListXML/JSON

      Extension applicablePeriodExt = list.getExtensionByUrl(Constants.ApplicablePeriodExtensionUrl);

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
      String system = identifierList.get(0).getSystem();
      String value = identifierList.get(0).getValue();
      DateTimeType startDate = applicablePeriod.getStartElement();
      DateTimeType endDate = applicablePeriod.getEndElement();
      String start = Helper.getFhirDate(LocalDateTime.of(startDate.getYear(), startDate.getMonth() + 1, startDate.getDay(), startDate.getHour(), startDate.getMinute(), startDate.getSecond()));
      String end = Helper.getFhirDate(LocalDateTime.of(endDate.getYear(), endDate.getMonth() + 1, endDate.getDay(), endDate.getHour(), endDate.getMinute(), endDate.getSecond()));
      Bundle bundle = this.getFhirDataProvider().findListByIdentifierAndDate(system, value, start, end);

      if (bundle.getEntry().size() == 0) {
        applicablePeriod.setStartElement(new DateTimeType(start));
        applicablePeriod.setEndElement(new DateTimeType(end));
        this.getFhirDataProvider().createResource(list);
      } else {
        ListResource existingList = (ListResource) bundle.getEntry().get(0).getResource();
        FhirHelper.mergeCensusLists(existingList, list);
        this.getFhirDataProvider().updateResource(existingList);
      }
    } else {
      String msg = "Only \"List\" resources are allowed";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    } /* else {
      this.getFhirDataProvider().createResource(resource);
    } */
  }
}
