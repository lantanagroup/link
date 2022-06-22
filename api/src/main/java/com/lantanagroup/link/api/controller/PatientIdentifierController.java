package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import com.google.common.annotations.VisibleForTesting;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.Helper;
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
   * @param reportTypeId - the type of the report (ex covid-min) and the format should be system|value
   */
  @PostMapping(value = "/csv", consumes = "text/csv")
  public void storeCSV(
          @RequestBody() String csvContent,
          @RequestParam String reportTypeId) throws Exception {
    logger.debug("Receiving RR FHIR CSV. Parsing...");
    try {
      if (reportTypeId == null || reportTypeId.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report Type should be provided.");
      }
      if (!reportTypeId.contains("|")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report type should be of format: system|value");
      }
      List<CsvEntry> list = this.getCsvEntries(csvContent);
        Map<String, List<CsvEntry>> csvMap = list.stream().collect(Collectors.groupingBy(CsvEntry::getPeriodIdentifier));
      for (String key : csvMap.keySet()) {
        ListResource listResource = getListResource(reportTypeId, csvMap.get(key));
        this.receiveFHIR(listResource);
      }
    } catch (ResponseStatusException ex) {
      logger.error(String.format("Error on storeCSV %s", ex.getMessage()), ex);
      throw ex;
    } catch (Exception ex) {
      logger.error(String.format("Error on storeCSV %s", ex.getMessage()), ex);
      throw ex;
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
    String system = list.getIdentifier().get(0).getSystem();
    String value = list.getIdentifier().get(0).getValue();;
    Bundle bundle = this.getFhirDataProvider().searchReportDefinition(system, value);
    if(bundle.getEntry().size() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report not Found.");
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
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient Identifier is required.");
        }
        if (!line[0].contains("|")) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient Identifier should be of format: system|value");
        }
        if (line[1] == null || line[1].isBlank()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date is required.");
        }
        SimpleDateFormat formatStartDate = new SimpleDateFormat("yyyy-MM-dd");
        try {
          formatStartDate.setLenient(false);
          formatStartDate.parse(line[1]);
        } catch (ParseException ex) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid start date. The start date format should be: YYYY-mm-dd.");
        }
        if (line[2] == null || line[1].isBlank()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date is required.");
        }
        SimpleDateFormat formatEndDate = new SimpleDateFormat("yyyy-MM-dd");
        try {
          formatEndDate.setLenient(false);
          formatEndDate.parse(line[2]);
        } catch (ParseException ex) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid end date. The end date format should be: YYYY-mm-dd.");
        }
        CsvEntry entry = new CsvEntry(line[0], line[1], line[2], line[3], line[4]);
        list.add(entry);
      }
    }
    if (list.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file should have at least one entry with data.");
    }
    //list.setExtension();
    return list;
  }

  private void receiveFHIR(Resource resource) throws Exception {
    logger.info("Storing patient identifiers");
    resource.setId((String) null);

    if (resource instanceof ListResource) {
      ListResource list = (ListResource) resource;

      List<Identifier> identifierList = ((ListResource) resource).getIdentifier();

      if (identifierList.isEmpty()) {
        throw new Exception("Identifier is not present.");
      }

      Extension applicablePeriodExt = list.getExtensionByUrl(Constants.ApplicablePeriodExtensionUrl);

      if (applicablePeriodExt == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "applicable-period extension is required");
      }

      Period applicablePeriod = applicablePeriodExt.getValue().castToPeriod(applicablePeriodExt.getValue());

      if (applicablePeriod == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "applicable-period extension must have a value of type Period");
      }

      if (!applicablePeriod.hasStart()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "applicable-period.start must have start");
      }

      if (!applicablePeriod.hasEnd()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "applicable-period.start must have end");
      }

      //system and value represents the measure intended for this patient id list
      String system = ((ListResource) resource).getIdentifier().get(0).getSystem();
      String value = ((ListResource) resource).getIdentifier().get(0).getValue();
      DateTimeType startDate = applicablePeriod.getStartElement();
      DateTimeType endDate = applicablePeriod.getEndElement();
      String start = Helper.getFhirDate(LocalDateTime.of(startDate.getYear(), startDate.getMonth() + 1, startDate.getDay(), startDate.getHour(), startDate.getMinute(), startDate.getSecond()));
      String end = Helper.getFhirDate(LocalDateTime.of(endDate.getYear(), endDate.getMonth() + 1, endDate.getDay(), endDate.getHour(), endDate.getMinute(), endDate.getSecond()));
      Bundle bundle = this.getFhirDataProvider().findListByIdentifierAndDate(system, value, start, end);

      if (bundle.getEntry().size() == 0) {
        applicablePeriod.setStartElement(new DateTimeType(start));
        applicablePeriod.setEndElement(new DateTimeType(end));
        this.getFhirDataProvider().createResource(resource);
      } else {
        ListResource existingList = (ListResource) bundle.getEntry().get(0).getResource();
        // filter out duplicates
        List<ListResource.ListEntryComponent> uniqueEntries = ((ListResource) resource).getEntry().parallelStream()
                .filter(e -> {

                  //check if reference or identifier exists, give priority to reference
                  String reference = e.getItem().getReference();
                  if(reference != null && !reference.isEmpty() && !reference.isBlank()) {
                    return !existingList.getEntry().stream().anyMatch(s -> reference.equals(s.getItem().getReference()));
                  }
                  else if(e.getItem().getIdentifier() != null) {
                    String systemA = e.getItem().getIdentifier().getSystem();
                    String valueA = e.getItem().getIdentifier().getValue();
                    return !existingList.getEntry().stream().anyMatch(s -> systemA.equals(s.getItem().getIdentifier().getSystem()) && valueA.equals(s.getItem().getIdentifier().getValue()));
                  }
                  else {
                    //log that no reference or id for patient
                    logger.info("No reference or identifier present for patient in provided resource.");
                    return false;
                  }
                }).collect(Collectors.toList());

        // merge lists into existingList
        uniqueEntries.parallelStream().forEach(entry -> {
          existingList.getEntry().add(entry);
        });
        this.getFhirDataProvider().updateResource(existingList);
      }
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only \"List\" resources are allowed");
    } /* else {
      this.getFhirDataProvider().createResource(resource);
    } */
  }

  private ListResource getListResource(String reportTypeId, List<CsvEntry> csvList) {
    ListResource list = new ListResource();
    List<Identifier> identifierList = new ArrayList<>();
    identifierList.add(new Identifier());
    identifierList.get(0).setSystem(reportTypeId.substring(0, reportTypeId.indexOf("|")));
    identifierList.get(0).setValue(reportTypeId.substring(reportTypeId.indexOf("|") + 1));
    list.setIdentifier(identifierList);
    List<Extension> applicablePeriodExtensionUrl = new ArrayList<>();
    applicablePeriodExtensionUrl.add(new Extension(Constants.ApplicablePeriodExtensionUrl));
    applicablePeriodExtensionUrl.get(0).setValue(csvList.get(0).getPeriod());
    list.setExtension(applicablePeriodExtensionUrl);
    //list.setDateElement(new DateTimeType(listDate));
    csvList.stream().parallel().forEach(csvEntry -> {
      ListResource.ListEntryComponent listEntry = new ListResource.ListEntryComponent();
      Reference reference = new Reference();
      if (csvEntry.getPatientLogicalID() != null && !csvEntry.getPatientLogicalID().isBlank()) {
        reference.setReference("Patient/" + csvEntry.getPatientLogicalID());
      }
      reference.setIdentifier(new Identifier());
      reference.getIdentifier().setSystemElement(new UriType(csvEntry.getPatientIdentifier().substring(0, csvEntry.getPatientIdentifier().indexOf("|"))));
      reference.getIdentifier().setValueElement(new StringType(csvEntry.getPatientIdentifier().substring(csvEntry.getPatientIdentifier().indexOf("|") + 1)));
      listEntry.setItem(reference);

      list.addEntry(listEntry);
    });
    return list;
  }
}
