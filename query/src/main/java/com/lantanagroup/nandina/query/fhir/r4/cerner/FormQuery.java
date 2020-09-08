package com.lantanagroup.nandina.query.fhir.r4.cerner;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.PIHCConstants;
import com.lantanagroup.nandina.query.BaseFormQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.report.*;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;

public class FormQuery extends BaseFormQuery {
  @Override
  public void execute() {
    EncounterScoop encounterScoop = (EncounterScoop) this.getContextData("scoopData");
    if (null == this.criteria.get("reportDate"))
      return;
    FhirContext ctx = (FhirContext) this.getContextData("fhirContext");

    Date reportDate = Date.valueOf(LocalDate.parse(this.criteria.get("reportDate")));

    PreviousDayAdmissionConfirmedCovidReport previousDayAdmissionConfirmedCovidReport = new PreviousDayAdmissionConfirmedCovidReport(encounterScoop, new ArrayList<>(), reportDate, ctx);
    this.setAnswer(PIHCConstants.PREVIOUS_DAY_ADMIT_CONFIRMED_COVID, previousDayAdmissionConfirmedCovidReport.getReportCount());

    PreviousDayAdmissionSuspectedCovidReport previousDayAdmissionSuspectedCovidReport = new PreviousDayAdmissionSuspectedCovidReport(encounterScoop, new ArrayList<>(), reportDate, ctx);
    this.setAnswer(PIHCConstants.PREVIOUS_DAY_ADMIT_SUSPECTED_COVID, previousDayAdmissionSuspectedCovidReport.getReportCount());

    OnsetReport previousDayOnset = new OnsetReport(encounterScoop, new ArrayList<>(), reportDate, LocalDate.now().minusDays(1), ctx);
    this.setAnswer(PIHCConstants.PREVIOUS_HOSPITAL_ONSET, previousDayOnset.getReportCount());

    // TODO: Previous Day Hospital Onset with Confirmed COVID

    // Hospitalized
    HospitalizedReport hospitalizedReport = new HospitalizedReport(encounterScoop, new ArrayList<>(), ctx);
    this.setAnswer(PIHCConstants.HOSPITALIZED, hospitalizedReport.getReportCount());

    // TODO: Hospitalized with Confirmed COVID

    // Hospitalized and Ventilated
    HospitalizedAndVentilatedReport hospitalizedAndVentilatedReport = new HospitalizedAndVentilatedReport(encounterScoop, new ArrayList<>(), ctx);
    this.setAnswer(PIHCConstants.HOSPITALIZED_AND_VENTILATED, hospitalizedAndVentilatedReport.getReportCount());

    // TODO: Hospitalized and Ventilated with Confirmed COVID

    OnsetReport onsetReport = new OnsetReport(encounterScoop, new ArrayList<>(), reportDate, ctx);
    this.setAnswer(PIHCConstants.HOSPITAL_ONSET, onsetReport.getReportCount());

    // TODO: Hospital Onset with Confirmed COVID

    // ED/Overflow
    EdOverflowReport edOverflowReport = new EdOverflowReport(encounterScoop, new ArrayList<>(), ctx);
    this.setAnswer(PIHCConstants.ED_OVERFLOW, edOverflowReport.getReportCount());

    // ED/Overflow with Confirmed COVID

    // ED/Overflow and Ventilated
    EdOverflowAndVentilatedReport edOverflowAndVentilatedReport = new EdOverflowAndVentilatedReport(encounterScoop, new ArrayList<>(), ctx);
    this.setAnswer(PIHCConstants.ED_OVERFLOW_AND_VENT, edOverflowAndVentilatedReport.getReportCount());

    // TODO: ED/Overflow and Ventilated with Confirmed COVID

    // Previous Day Deaths
    DeathReport deathReport = new DeathReport(encounterScoop, new ArrayList<>(), reportDate, ctx);
    this.setAnswer(PIHCConstants.PREVIOUS_DAY_DEATHS, deathReport.getReportCount());

    // TODO: Previous Day Deaths with Confirmed COVID
  }
}
