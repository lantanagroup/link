package com.lantanagroup.link.api.controller;

import com.google.common.annotations.VisibleForTesting;
import com.lantanagroup.link.*;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.model.CsvEntry;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reportability Response Controller
 */
@RestController
@RequestMapping("/api/poi")
public class PatientIdentifierController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(PatientIdentifierController.class);

  /**
   * Posts a csv file with Patient Identifiers and Dates to the Fhir server.
   * @param csvContent The content of the CSV
   * @param listIdentifier - the type of the report (ex covid-min) and the format should be system|value
   */
  @PostMapping(value = "/csv", consumes = "text/csv")
  public void storeCSV(
          @RequestBody() String csvContent,
          @RequestParam String listIdentifier) throws Exception {
    logger.debug("Receiving Patient List CSV. Parsing...");
    if (listIdentifier == null || listIdentifier.isBlank()) {
      String msg = "List Identifier should be provided.";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
    if (!listIdentifier.contains("|")) {
      String msg = "List Identifier should be of format: system|value";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
    List<CsvEntry> list = this.getCsvEntries(csvContent);
    Map<String, List<CsvEntry>> csvMap = list.stream().collect(Collectors.groupingBy(CsvEntry::getPeriodIdentifier));
    for (String key : csvMap.keySet()) {
      ListResource listResource = getListResource(listIdentifier, csvMap.get(key));
      checkMeasureIdentifier(listResource);
      this.receiveFHIR(listResource);
    }
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
      String msg = String.format("Measure Identified With Value '%s' and System '%s' not found on CQF Evaluation Service", measureIdentifier.getValue(), measureIdentifier.getSystem());
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }
  }

  @VisibleForTesting
  List<CsvEntry> getCsvEntries(String csvContent) throws IOException, CsvValidationException {
    InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    CSVReader csvReader = new CSVReaderBuilder(bufferedReader).withSkipLines(1).build();
    List<CsvEntry> list = new ArrayList<>();
    String[] line;
    while ((line = csvReader.readNext()) != null) {
      if (line.length > 0) {
        if (line[0] == null || line[0].isBlank()) {
          String msg = "Patient Identifier is required in CSV census import.";
          logger.error(msg);
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        if (!line[0].contains("|")) {
          String msg = "Patient Identifier in CSV census import should be of format: system|value";
          logger.error(msg);
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        if (line[1] == null || line[1].isBlank()) {
          String msg = "Start date is required in CSV census import.";
          logger.error(msg);
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        SimpleDateFormat formatStartDate = new SimpleDateFormat("yyyy-MM-dd");
        try {
          formatStartDate.setLenient(false);
          formatStartDate.parse(line[1]);
        } catch (ParseException ex) {
          String msg = "Invalid start date in CSV census import. The start date format should be: YYYY-mm-dd.";
          logger.error(msg);
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        if (line[2] == null || line[1].isBlank()) {
          String msg = "End date is required in CSV census import.";
          logger.error(msg);
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        SimpleDateFormat formatEndDate = new SimpleDateFormat("yyyy-MM-dd");
        try {
          formatEndDate.setLenient(false);
          formatEndDate.parse(line[2]);
        } catch (ParseException ex) {
          String msg = "Invalid end date format in CSV census import. The end date format should be: YYYY-mm-dd.";
          logger.error(msg);
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        CsvEntry entry = new CsvEntry(line[0], line[1], line[2], line[3]);
        list.add(entry);
      }
    }
    if (list.isEmpty()) {
      String msg = "The file should have at least one entry with data in CSV census import.";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
    //list.setExtension();
    return list;
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

  private ListResource getListResource(String reportTypeId, List<CsvEntry> csvList) {
    ListResource list = new ListResource();
    list.addIdentifier(IdentifierHelper.fromString(reportTypeId));
    List<Extension> applicablePeriodExtensionUrl = new ArrayList<>();
    applicablePeriodExtensionUrl.add(new Extension(Constants.ApplicablePeriodExtensionUrl));
    applicablePeriodExtensionUrl.get(0).setValue(csvList.get(0).getPeriod());
    list.setExtension(applicablePeriodExtensionUrl);
    //list.setDateElement(new DateTimeType(listDate));
    list.setStatus(ListResource.ListStatus.CURRENT);
    list.setMode(ListResource.ListMode.WORKING);
    list.setTitle(String.format("Census List for %s", IdentifierHelper.fromString(reportTypeId).getValue()));

    CodeableConcept cc = new CodeableConcept();
    cc.setText("PatientList");
    list.setCode(cc);

    list.setDate(new Date());

    csvList.stream().parallel().forEach(csvEntry -> {
      ListResource.ListEntryComponent listEntry = new ListResource.ListEntryComponent();
      Reference reference = new Reference();
      if (csvEntry.getPatientLogicalID() != null && !csvEntry.getPatientLogicalID().isBlank()) {
        reference.setReference("Patient/" + csvEntry.getPatientLogicalID());
      }
      reference.setIdentifier(IdentifierHelper.fromString(csvEntry.getPatientIdentifier()));
      listEntry.setItem(reference);

      list.addEntry(listEntry);
    });
    return list;
  }
}
