package com.lantanagroup.nandina;

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

    resp.addItem(this.createAnswer(PIHCConstants.FACILITY_ID, "facility-id", "Facility ID", UriType.class));
    resp.addItem(this.createAnswer(PIHCConstants.SUMMARY_CENSUS_ID, "summary-census-id", "Summary Census ID", UriType.class));
    resp.addItem(this.createAnswer("date", "collection-date", "Date for which patient impact and hospital capacity counts are recorded", DateType.class));
    
    QuestionnaireResponse.QuestionnaireResponseItemComponent section1 = resp.addItem();
    section1.setLinkId("covid-19-patient-impact-group");
    section1.setText("Patient Impact Section");

    QuestionnaireResponse.QuestionnaireResponseItemComponent previousDayAdmit = this.createAnswer(PIHCConstants.PREVIOUS_DAY_ADMIT_CONFIRMED_COVID, PIHCConstants.PREVIOUS_DAY_ADMIT_CONFIRMED_COVID, "PREVIOUS DAY’S ADMISSIONS WITH CONFIRMED COVID-19: New patients admitted to an inpatient bed who had confirmed COVID-19 at the time of admission", IntegerType.class);
    section1.addItem(previousDayAdmit);
    previousDayAdmit.addItem(this.createAnswer(PIHCConstants.PREVIOUS_DAY_ADMIT_SUSPECTED_COVID, PIHCConstants.PREVIOUS_DAY_ADMIT_SUSPECTED_COVID, "PREVIOUS DAY’S ADMISSIONS WITH SUSPECTED COVID-19: New patients admitted to an inpatient bed who had suspected COVID-19 at the time of admission", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent previousHospitalOnset = this.createAnswer(PIHCConstants.PREVIOUS_HOSPITAL_ONSET, PIHCConstants.PREVIOUS_HOSPITAL_ONSET, "PREVIOUS DAY’S NEW HOSPITAL ONSET: Current inpatients hospitalized for a condition other than COVID-19 with onset of suspected or confirmed COVID-19 on the previous day and the previous day is fourteen or more days since admission", IntegerType.class);
    section1.addItem(previousHospitalOnset);
    previousHospitalOnset.addItem(this.createAnswer(PIHCConstants.PREVIOUS_HOSPITAL_ONSET_CONFIRMED_COVID, PIHCConstants.PREVIOUS_HOSPITAL_ONSET_CONFIRMED_COVID, "Number of Previous Day’s New Hospital Onset with Confirmed COVID-19 (subset)", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent hospitalized = this.createAnswer(PIHCConstants.HOSPITALIZED, PIHCConstants.HOSPITALIZED, "HOSPITALIZED: Patients currently hospitalized in an inpatient bed who have suspected or confirmed COVID-19", IntegerType.class);
    section1.addItem(hospitalized);
    hospitalized.addItem(this.createAnswer(PIHCConstants.HOSPITALIZED_CONFIRMED_COVID, PIHCConstants.HOSPITALIZED_CONFIRMED_COVID, "Number of Hospitalized with Confirmed COVID-19 (subset)", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent hospitalizedAndVent = this.createAnswer(PIHCConstants.HOSPITALIZED_AND_VENTILATED, PIHCConstants.HOSPITALIZED_AND_VENTILATED, "HOSPITALIZED and VENTILATED: Patients currently hospitalized in an inpatient bed who have suspected or confirmed COVID-19 and are on a mechanical ventilator", IntegerType.class);
    section1.addItem(hospitalizedAndVent);
    hospitalizedAndVent.addItem(this.createAnswer(PIHCConstants.HOSPITALIZED_AND_VENTILATED_CONFIRMED_COVID, PIHCConstants.HOSPITALIZED_AND_VENTILATED_CONFIRMED_COVID, "Number of Hospitalized and Ventilated with Confirmed COVID-19 (subset)", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent hospitalOnset = this.createAnswer(PIHCConstants.HOSPITAL_ONSET, PIHCConstants.HOSPITAL_ONSET, "HOSPITAL ONSET: Total current inpatients with onset of suspected or confirmed COVID-19 fourteen or more days after admission for a condition other than COVID-19", IntegerType.class);
    section1.addItem(hospitalOnset);
    hospitalOnset.addItem(this.createAnswer(PIHCConstants.HOSPITAL_ONSET_CONFIRMED_COVID, PIHCConstants.HOSPITAL_ONSET_CONFIRMED_COVID, "Number of Hospital Onset with Confirmed COVID-19 (subset)", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent edOverflow = this.createAnswer(PIHCConstants.ED_OVERFLOW, PIHCConstants.ED_OVERFLOW, "ED/OVERFLOW: Patients with suspected or confirmed COVID-19 who currently are in the Emergency Department (ED) or any overflow location awaiting an inpatient bed", IntegerType.class);
    section1.addItem(edOverflow);
    edOverflow.addItem(this.createAnswer(PIHCConstants.ED_OVERFLOW_CONFIRMED_COVID, PIHCConstants.ED_OVERFLOW_CONFIRMED_COVID, "Number of ED/Overflow with Confirmed COVID-19 (subset)", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent edOverflowAndVent = this.createAnswer(PIHCConstants.ED_OVERFLOW_AND_VENT, PIHCConstants.ED_OVERFLOW_AND_VENT, "ED/OVERFLOW and VENTILATED: Patients with suspected or confirmed COVID-19 who currently are in the ED or any overflow location awaiting an inpatient bed and on a mechanical ventilator", IntegerType.class);
    section1.addItem(edOverflowAndVent);
    edOverflowAndVent.addItem(this.createAnswer(PIHCConstants.ED_OVERFLOW_AND_VENT_CONFIRMED_COVID, PIHCConstants.ED_OVERFLOW_AND_VENT_CONFIRMED_COVID, "Number of ED/Overflow and Ventilated with Confirmed COVID-19 (subset)", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent previousDayDeaths = this.createAnswer(PIHCConstants.PREVIOUS_DAY_DEATHS, PIHCConstants.PREVIOUS_DAY_DEATHS, "PREVIOUS DAY’S DEATHS: Patients with suspected or confirmed COVID-19 who died in the hospital, ED, or any overflow location on the previous calendar day", IntegerType.class);
    section1.addItem(previousDayDeaths);
    previousDayDeaths.addItem(this.createAnswer(PIHCConstants.PREVIOUS_DAY_DEATHS_CONFIRMED_COVID, PIHCConstants.PREVIOUS_DAY_DEATHS_CONFIRMED_COVID, "Number of Previous Day’s Deaths with Confirmed COVID-19 (subset)", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent section2 = resp.addItem();
    section2.setLinkId("hospital-bed-icu-ventilator-capacity-group");
    section2.setText("Hospital Bed/ICU/Ventilator Capacity Section");

    section2.addItem(this.createAnswer(PIHCConstants.ALL_HOSPITAL_BEDS, PIHCConstants.ALL_HOSPITAL_BEDS, "ALL HOSPITAL BEDS: Total number of all staffed inpatient and outpatient beds in your hospital, including all overflow and surge/expansion beds used for inpatients and for outpatients (includes all ICU beds)", IntegerType.class));
    section2.addItem(this.createAnswer(PIHCConstants.HOSPITAL_INPATIENT_BEDS, PIHCConstants.HOSPITAL_INPATIENT_BEDS, "HOSPITAL INPATIENT BEDS: Total number of staffed inpatient beds in your hospital including all overflow and surge/ expansion beds used for inpatients (includes all ICU beds)", IntegerType.class));
    section2.addItem(this.createAnswer(PIHCConstants.HOSPITAL_INPATIENT_BED_OCC, PIHCConstants.HOSPITAL_INPATIENT_BED_OCC, "HOSPITAL INPATIENT BED OCCUPANCY: Total number of staffed inpatient beds that are occupied", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent hospitalIcuBeds = this.createAnswer(PIHCConstants.HOSPITAL_ICU_BEDS, PIHCConstants.HOSPITAL_ICU_BEDS, "ICU BEDS: Total number of staffed inpatient ICU beds", IntegerType.class);
    section2.addItem(hospitalIcuBeds);
    hospitalIcuBeds.addItem(this.createAnswer(PIHCConstants.HOSPITAL_NICU_BEDS, PIHCConstants.HOSPITAL_NICU_BEDS, "Number of ICU Beds that are Neonatal Beds (subset)", IntegerType.class));

    QuestionnaireResponse.QuestionnaireResponseItemComponent hospitalIcuBedOcc = this.createAnswer(PIHCConstants.HOSPITAL_ICU_BED_OCC, PIHCConstants.HOSPITAL_ICU_BED_OCC, "ICU BED OCCUPANCY: Total number of staffed inpatient ICU beds that are occupied", IntegerType.class);
    section2.addItem(hospitalIcuBedOcc);
    hospitalIcuBedOcc.addItem(this.createAnswer(PIHCConstants.HOSPITAL_NICU_BED_OCC, PIHCConstants.HOSPITAL_NICU_BED_OCC, "Number of Occupied ICU Beds that are Neonatal Beds (subset)", IntegerType.class));

    section2.addItem(this.createAnswer(PIHCConstants.MECHANICAL_VENTILATORS, PIHCConstants.MECHANICAL_VENTILATORS, "MECHANICAL VENTILATORS: Total number of ventilators available", IntegerType.class));
    section2.addItem(this.createAnswer(PIHCConstants.MECHANICAL_VENTILATORS_USED, PIHCConstants.MECHANICAL_VENTILATORS_USED, "MECHANICAL VENTILATORS IN USE: Total number of ventilators in use", IntegerType.class));

    return resp;
  }
}
