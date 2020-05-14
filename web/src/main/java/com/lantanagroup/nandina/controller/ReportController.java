package com.lantanagroup.nandina.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.TransformHelper;
import com.lantanagroup.nandina.hapi.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.nandina.model.QuestionnaireResponseSimple;
import com.lantanagroup.nandina.query.IQueryCountExecutor;
import com.lantanagroup.nandina.query.QueryFactory;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ReportController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
  FhirContext ctx = FhirContext.forR4();
  IGenericClient fhirClient;

  public ReportController() {
    this.fhirClient = this.ctx.newRestfulGenericClient(Config.getInstance().getFhirServerBase());
    this.fhirClient.registerInterceptor(new HapiFhirAuthenticationInterceptor());
  }

  /**
   * Uses reflection to determine what class should be used to execute the requested query/className, and
   * executes the specified query, returning the result.
   * @param className
   * @param reportDate
   * @param overflowLocations
   * @return
   */
  private Integer executeQueryCount(String className, Map<String, String> criteria) {
    if (className == null || className.isEmpty()) return null;

    try {
      IQueryCountExecutor executor = (IQueryCountExecutor) QueryFactory.newInstance(className, Config.getInstance(), fhirClient, criteria);
      return executor.execute();
    } catch (ClassNotFoundException ex) {
      logger.error("Could not find class for query named " + className, ex);
    } catch (Exception ex) {
      logger.error("Could not execute query class for query " + className, ex);
    }

    return null;
  }

  private Map<String, String> getCriteria(HttpServletRequest request) {
    java.util.Enumeration<String> parameterNames = request.getParameterNames();
    Map<String, String> criteria = new HashMap<>();

    while (parameterNames.hasMoreElements()) {
      String paramName = parameterNames.nextElement();
      String[] paramValues = request.getParameterValues(paramName);
      criteria.put(paramName, String.join(",", paramValues));
    }

    return criteria;
  }

  @GetMapping("/api/query")
  public QuestionnaireResponseSimple getQuestionnaireResponse(HttpServletRequest request) {
    QuestionnaireResponseSimple response = new QuestionnaireResponseSimple();

    Map<String, String> criteria = this.getCriteria(request);

    String reportDate = criteria.get("reportDate");
    response.setDate(reportDate);

    logger.trace("Generating report, including criteria: " + criteria.toString());

    if (!Helper.isNullOrEmpty(Config.getInstance().getFieldDefaultFacilityId())) {
      response.setFacilityId(Config.getInstance().getFieldDefaultFacilityId());
    }

    if (!Helper.isNullOrEmpty(Config.getInstance().getFieldDefaultSummaryCensusId())) {
      response.setSummaryCensusId(Config.getInstance().getFieldDefaultSummaryCensusId());
    }

    Integer hospitalizedTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalized(), criteria);
    response.setHospitalized(hospitalizedTotal);

    Integer hospitalizedAndVentilatedTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalizedAndVentilated(), criteria);
    response.setHospitalizedAndVentilated(hospitalizedAndVentilatedTotal);

    Integer hospitalOnsetTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalOnset(), criteria);
    response.setHospitalOnset(hospitalOnsetTotal);

    Integer edOverflowTotal = this.executeQueryCount(Config.getInstance().getQueryEDOverflow(), criteria);
    response.setEdOverflow(edOverflowTotal);

    Integer edOverflowAndVentilatedTotal = this.executeQueryCount(Config.getInstance().getQueryEDOverflowAndVentilated(), criteria);
    response.setEdOverflowAndVentilated(edOverflowAndVentilatedTotal);

    Integer deathsTotal = this.executeQueryCount(Config.getInstance().getQueryDeaths(), criteria);
    response.setDeaths(deathsTotal);

    Integer hospitalBedsTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalBeds(), criteria);
    response.setAllHospitalBeds(hospitalBedsTotal);

    Integer hospitalInpatientBedsTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalInpatientBeds(), criteria);
    response.setHospitalInpatientBeds(hospitalInpatientBedsTotal);

    Integer hospitalInpatientBedOccTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalInpatientBedOcc(), criteria);
    response.setHospitalInpatientBedOccupancy(hospitalInpatientBedOccTotal);

    Integer hospitalIcuBedsTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalIcuBeds(), criteria);
    response.setIcuBeds(hospitalIcuBedsTotal);
    
    Integer hospitalIcuBedOccTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalIcuBedOcc(), criteria);
    response.setIcuBedOccupancy(hospitalIcuBedOccTotal);

    Integer mechanicalVentilatorsTotal = this.executeQueryCount(Config.getInstance().getQueryMechanicalVentilators(), criteria);
    response.setMechanicalVentilators(mechanicalVentilatorsTotal);

    Integer mechanicalVentilatorsUsedTotal = this.executeQueryCount(Config.getInstance().getQueryMechanicalVentilatorsUsed(), criteria);
    response.setMechanicalVentilatorsInUse(mechanicalVentilatorsUsedTotal);

    return response;
  }

  private QuestionnaireResponse.QuestionnaireResponseItemComponent createItemAnswer(String linkId, String text, Type value) {
    QuestionnaireResponse.QuestionnaireResponseItemComponent item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();
    item.setLinkId(linkId);
    item.setText(text);

    QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answer = item.addAnswer();
    answer.setValue(value);

    return item;
  }

  private QuestionnaireResponse createQuestionnaireResponse(QuestionnaireResponseSimple simple) {
    QuestionnaireResponse resp = new QuestionnaireResponse();

    resp.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
    resp.setQuestionnaire("http://hl7.org/fhir/us/hai/Questionnaire/hai-questionnaire-covid-19-pt-impact-hosp-capacity");

    if (!Helper.isNullOrEmpty(simple.getFacilityId())) {
      resp.addItem(this.createItemAnswer(
              "facility-id",
              "Facility ID",
              new UriType(simple.getFacilityId())));
    }

    if (!Helper.isNullOrEmpty(simple.getSummaryCensusId())) {
      resp.addItem(this.createItemAnswer(
              "summary-census-id",
              "Summary Census ID",
              new UriType(simple.getSummaryCensusId())));
    }

    if (!Helper.isNullOrEmpty(simple.getDate())) {
      resp.addItem(this.createItemAnswer(
              "collection-date",
              "Date for which patient impact and hospital capacity counts are recorded",
              new DateType(simple.getDate())));
    }

    QuestionnaireResponse.QuestionnaireResponseItemComponent section1 = new QuestionnaireResponse.QuestionnaireResponseItemComponent();
    section1.setLinkId("covid-19-patient-impact-group");
    section1.setText("Patient Impact Section");

    if (simple.getHospitalized() != null) {
      section1.addItem(this.createItemAnswer(
              "numC19HospPats",
              "Patients currently hospitalized in an inpatient bed who have suspected or confirmed COVID-19",
              new IntegerType(simple.getHospitalized())));
    }

    if (simple.getHospitalizedAndVentilated() != null) {
      section1.addItem(this.createItemAnswer(
              "numC19MechVentPats",
              "Patients currently hospitalized in an inpatient bed who have suspected or confirmed COVID-19 and are on a mechanical ventilator",
              new IntegerType(simple.getHospitalizedAndVentilated())));
    }

    if (simple.getHospitalOnset() != null) {
      section1.addItem(this.createItemAnswer(
              "numC19HOPats",
              "Patients currently hospitalized in an inpatient bed with onset of suspected or confirmed COVID-19 fourteen or more days after hospital admission due to a condition other than COVID-19",
              new IntegerType(simple.getHospitalOnset())));
    }

    if (simple.getEdOverflow() != null) {
      section1.addItem(this.createItemAnswer(
              "numC19OverflowPats",
              "Patients with suspected or confirmed COVID-19 who are currently in the Emergency Department (ED) or any overflow location awaiting an inpatient bed",
              new IntegerType(simple.getEdOverflow())));
    }

    if (simple.getEdOverflowAndVentilated() != null) {
      section1.addItem(this.createItemAnswer(
              "numC19OFMechVentPats",
              "Patients with suspected or confirmed COVID-19 who currently are in the ED or any overflow location awaiting an inpatient bed and on a mechanical ventilator",
              new IntegerType(simple.getEdOverflowAndVentilated())));
    }

    if (simple.getDeaths() != null) {
      section1.addItem(this.createItemAnswer(
              "numC19Died",
              "Patients with suspected or confirmed COVID-19 who died in the hospital, ED or any overflow location on the date for which you are reporting",
              new IntegerType(simple.getDeaths())));
    }

    if (section1.getItem().size() > 0) {
      resp.addItem(section1);
    }

    // Section 2
    QuestionnaireResponse.QuestionnaireResponseItemComponent section2 = new QuestionnaireResponse.QuestionnaireResponseItemComponent();
    section2.setLinkId("hospital-bed-icu-ventilator-capacity-group");
    section2.setText("Hospital Bed/ICU/Ventilator Capacity Section");

    if (simple.getAllHospitalBeds() != null) {
      section2.addItem(this.createItemAnswer(
              "numTotBeds",
              "Total number of all inpatient and outpatient beds in your hospital, including all staffed, licensed, and overflow surge or expansion beds used for inpatients or for outpatients (includes ICU beds)",
              new IntegerType(simple.getAllHospitalBeds())));
    }

    if (simple.getHospitalInpatientBeds() != null) {
      section2.addItem(this.createItemAnswer(
              "numBeds",
              "Total number of staffed inpatient beds in your hospital, including all staffed, licensed, and overflow and surge or expansion beds used for inpatients (includes ICU beds)",
              new IntegerType(simple.getHospitalInpatientBeds())));
    }

    if (simple.getHospitalInpatientBedOccupancy() != null) {
      section2.addItem(this.createItemAnswer(
              "numBedsOcc",
              "Total number of staffed inpatient beds that are currently occupied",
              new IntegerType(simple.getHospitalInpatientBedOccupancy())));
    }

    if (simple.getIcuBeds() != null) {
      section2.addItem(this.createItemAnswer(
              "numICUBeds",
              "Total number of staffed inpatient intensive care unit (ICU) beds",
              new IntegerType(simple.getIcuBeds())));
    }

    if (simple.getIcuBedOccupancy() != null) {
      section2.addItem(this.createItemAnswer(
              "numICUBedsOcc",
              "Total number of staffed inpatient ICU beds that are occupied",
              new IntegerType(simple.getIcuBedOccupancy())));
    }

    if (simple.getMechanicalVentilators() != null) {
      section2.addItem(this.createItemAnswer(
              "numVent",
              "Total number of ventilators available",
              new IntegerType(simple.getMechanicalVentilators())));
    }

    if (simple.getMechanicalVentilatorsInUse() != null) {
      section2.addItem(this.createItemAnswer(
              "numVentUse",
              "Total number of ventilators in use",
              new IntegerType(simple.getMechanicalVentilatorsInUse())));
    }

    if (section2.getItem().size() > 0) {
      resp.addItem(section2);
    }

    return resp;
  }

  private String convertToCSV(QuestionnaireResponse questionnaireResponse) throws TransformerException, FileNotFoundException {
    String xml = this.ctx.newXmlParser().encodeResourceToString(questionnaireResponse);
    TransformHelper transformHelper = new TransformHelper();
    return transformHelper.convert(xml);
  }

  @PostMapping("/api/convert")
  public void convertSimpleReport(@RequestBody() QuestionnaireResponseSimple body, HttpServletResponse response) throws IOException, TransformerException {
    QuestionnaireResponse questionnaireResponse = this.createQuestionnaireResponse(body);
    String responseBody = null;

    if (Config.getInstance().getExportFormat().equals("json")) {
      responseBody = this.ctx.newJsonParser().encodeResourceToString(questionnaireResponse);
      response.setContentType("application/json");
      response.setHeader("Content-Disposition", "attachment; filename=\"report.json\"");
    } else if (Config.getInstance().getExportFormat().equals("xml")) {
      responseBody = this.ctx.newXmlParser().encodeResourceToString(questionnaireResponse);
      response.setContentType("application/xml");
      response.setHeader("Content-Disposition", "attachment; filename=\"report.xml\"");
    } else {
      responseBody = this.convertToCSV(questionnaireResponse);
      response.setContentType("text/plain");
      response.setHeader("Content-Disposition", "attachment; filename=\"report.csv\"");
    }

    InputStream is = new ByteArrayInputStream(responseBody.getBytes());
    IOUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }
}
