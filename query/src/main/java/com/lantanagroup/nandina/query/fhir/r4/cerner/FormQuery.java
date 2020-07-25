package com.lantanagroup.nandina.query.fhir.r4.cerner;

import com.lantanagroup.nandina.PIHCConstants;
import com.lantanagroup.nandina.query.BaseFormQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.report.*;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.time.LocalDate;
import java.util.ArrayList;

public class FormQuery extends BaseFormQuery {
  @Override
  public void execute() {
    EncounterScoop encounterScoop = (EncounterScoop) this.getContextData("scoopData");

    // TODO: Prev Day Admitted with Confirmed COVID

    // TODO: Prev Day Admitted with Suspected COVID

    // TODO: Previous Day Hospital Onset

    // TODO: Previous Day Hospital Onset with Confirmed COVID

    // Hospitalized
    HospitalizedReport hospitalizedReport = new HospitalizedReport(encounterScoop, new ArrayList<>());
    this.setAnswer(PIHCConstants.HOSPITALIZED, hospitalizedReport.getReportCount());

    // TODO: Hospitalized with Confirmed COVID

    // Hospitalized and Ventilated
    HospitalizedAndVentilatedReport hospitalizedAndVentilatedReport = new HospitalizedAndVentilatedReport(encounterScoop, new ArrayList<>());
    this.setAnswer(PIHCConstants.HOSPITALIZED_AND_VENTILATED, hospitalizedAndVentilatedReport.getReportCount());

    // TODO: Hospitalized and Ventilated with Confirmed COVID

    // TODO: Hospital Onset

    // TODO: Hospital Onset with Confirmed COVID

    // ED/Overflow
    EdOverflowReport edOverflowReport = new EdOverflowReport(encounterScoop, new ArrayList<>());
    this.setAnswer(PIHCConstants.ED_OVERFLOW, edOverflowReport.getReportCount());

    // ED/Overflow with Confirmed COVID

    // ED/Overflow and Ventilated
    EdOverflowAndVentilatedReport edOverflowAndVentilatedReport = new EdOverflowAndVentilatedReport(encounterScoop, new ArrayList<>());
    this.setAnswer(PIHCConstants.ED_OVERFLOW_AND_VENT, edOverflowAndVentilatedReport.getReportCount());

    // TODO: ED/Overflow and Ventilated with Confirmed COVID

    // Previous Day Deaths
    // TODO: Needs to use report date (the date the user selected), instead of LocalDate.now()
    DeathReport deathReport = new DeathReport(encounterScoop, new ArrayList<>(), java.sql.Date.valueOf(LocalDate.now().minusDays(1)));
    this.setAnswer(PIHCConstants.PREVIOUS_DAY_DEATHS, deathReport.getReportCount());

    // TODO: Previous Day Deaths with Confirmed COVID
  }
}
