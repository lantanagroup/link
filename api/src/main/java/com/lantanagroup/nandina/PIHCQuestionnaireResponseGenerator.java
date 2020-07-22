package com.lantanagroup.nandina;

import com.lantanagroup.nandina.model.QueryReport;
import org.hl7.fhir.r4.model.*;

public class PIHCQuestionnaireResponseGenerator {
  private QueryReport report;

  public PIHCQuestionnaireResponseGenerator(QueryReport report) {
    this.report = report;
  }

  private QuestionnaireResponse.QuestionnaireResponseItemComponent createItem(String linkId, String text) {
    QuestionnaireResponse.QuestionnaireResponseItemComponent item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();
    item.setLinkId(linkId);
    item.setText(text);
    return item;
  }

  private QuestionnaireResponse.QuestionnaireResponseItemComponent createAnswer(String questionId, String linkId, String text, Class answerType) {
    QuestionnaireResponse.QuestionnaireResponseItemComponent item = this.createItem(linkId, text);
    QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answer = item.addAnswer();
    Object answerValue = report.getAnswer(questionId);

    if (answerValue != null) {
      if (answerType == StringType.class) {
        if (answerValue.getClass() == String.class) {
          answer.setValue(new StringType((String) answerValue));
        } else {
          throw new UnsupportedOperationException("Answers of type " + answerType.getName() + " with a value of type = " + answerValue.getClass().getName() + " are not supported");
        }
      } else if (answerType == DateType.class || answerType == DateTimeType.class) {
        if (answerValue.getClass() == String.class) {
          answer.setValue(new DateType((String) answerValue));
        } else {
          throw new UnsupportedOperationException("Answers of type " + answerType.getName() + " with a value of type = " + answerValue.getClass().getName() + " are not supported");
        }
      } else if (answerType == IntegerType.class) {
        if (answerValue.getClass() == Integer.class) {
          answer.setValue(new IntegerType((Integer) answerValue));
        } else {
          throw new UnsupportedOperationException("Answers of type " + answerType.getName() + " with a value of type = " + answerValue.getClass().getName() + " are not supported");
        }
      } else if (answerType == UriType.class) {
        if (answerValue.getClass() == String.class) {
          answer.setValue(new UriType((String) answerValue));
        } else {
          throw new UnsupportedOperationException("Answers of type " + answerType.getName() + " with a value of type = " + answerValue.getClass().getName() + " are not supported");
        }
      } else {
        throw new UnsupportedOperationException("Answers of type " + answerType.getName() + " are not supported");
      }
    }

    return item;
  }

  public QuestionnaireResponse generate() {
    QuestionnaireResponse resp = new QuestionnaireResponse();

    resp.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
    resp.setQuestionnaire("http://hl7.org/fhir/us/hai/Questionnaire/hai-questionnaire-covid-19-pt-impact-hosp-capacity");

    resp.addItem(this.createAnswer(Constants.FACILITY_ID, "facility-id", "Facility ID", UriType.class));
    resp.addItem(this.createAnswer(Constants.SUMMARY_CENSUS_ID, "summary-census-id", "Summary Census ID", UriType.class));
    resp.addItem(this.createAnswer("date", "collection-date", "Date for which patient impact and hospital capacity counts are recorded", DateType.class));
    
    QuestionnaireResponse.QuestionnaireResponseItemComponent section1 = resp.addItem();
    section1.setLinkId("covid-19-patient-impact-group");
    section1.setText("Patient Impact Section");

    section1.addItem(this.createAnswer(Constants.HOSPITALIZED, "numC19HospPats", "Patients currently hospitalized in an inpatient bed who have suspected or confirmed COVID-19", IntegerType.class));
    section1.addItem(this.createAnswer(Constants.HOSPITALIZED_AND_VENTILATED, "numC19MechVentPats", "Patients currently hospitalized in an inpatient bed who have suspected or confirmed COVID-19 and are on a mechanical ventilator", IntegerType.class));
    section1.addItem(this.createAnswer(Constants.HOSPITAL_ONSET, "numC19HOPats", "Patients currently hospitalized in an inpatient bed with onset of suspected or confirmed COVID-19 fourteen or more days after hospital admission due to a condition other than COVID-19", IntegerType.class));
    section1.addItem(this.createAnswer(Constants.ED_OVERFLOW, "numC19OverflowPats", "Patients with suspected or confirmed COVID-19 who are currently in the Emergency Department (ED) or any overflow location awaiting an inpatient bed", IntegerType.class));
    section1.addItem(this.createAnswer(Constants.ED_OVERFLOW_AND_VENTILATED, "numC19OFMechVentPats", "Patients with suspected or confirmed COVID-19 who currently are in the ED or any overflow location awaiting an inpatient bed and on a mechanical ventilator", IntegerType.class));
    section1.addItem(this.createAnswer(Constants.PREV_DAYS_DEATHS, "numC19Died", "Patients with suspected or confirmed COVID-19 who died in the hospital, ED or any overflow location on the date for which you are reporting", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent section2 = resp.addItem();
    section2.setLinkId("hospital-bed-icu-ventilator-capacity-group");
    section2.setText("Hospital Bed/ICU/Ventilator Capacity Section");

    section2.addItem(this.createAnswer(Constants.HOSPITAL_BEDS, "numTotBeds", "Total number of all inpatient and outpatient beds in your hospital, including all staffed, licensed, and overflow surge or expansion beds used for inpatients or for outpatients (includes ICU beds)", IntegerType.class));
    section2.addItem(this.createAnswer(Constants.HOSPITAL_INPATIENT_BEDS, "numBeds", "Total number of staffed inpatient beds in your hospital, including all staffed, licensed, and overflow and surge or expansion beds used for inpatients (includes ICU beds)", IntegerType.class));
    section2.addItem(this.createAnswer(Constants.HOSPITAL_INPATIENT_BED_OCC, "numBedsOcc", "Total number of staffed inpatient beds that are currently occupied", IntegerType.class));
    section2.addItem(this.createAnswer(Constants.HOSPITAL_ICU_BEDS, "numICUBeds", "Total number of staffed inpatient intensive care unit (ICU) beds", IntegerType.class));
    section2.addItem(this.createAnswer(Constants.HOSPITAL_ICU_BED_OCC, "numICUBedsOcc", "Total number of staffed inpatient ICU beds that are occupied", IntegerType.class));
    section2.addItem(this.createAnswer(Constants.MECHANICAL_VENTILATORS, "numVent", "Total number of ventilators available", IntegerType.class));
    section2.addItem(this.createAnswer(Constants.MECHANICAL_VENTILATORS_USED, "numVentUse", "Total number of ventilators in use", IntegerType.class));

    return resp;
  }
}
