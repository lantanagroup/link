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

    // Hospitalized
    HospitalizedReport hospitalizedReport = new HospitalizedReport(encounterScoop, new ArrayList<>());
    this.setAnswer(PIHCConstants.HOSPITALIZED, hospitalizedReport.getReportCount());

    // Hospitalized and Ventilated
    HospitalizedAndVentilatedReport hospitalizedAndVentilatedReport = new HospitalizedAndVentilatedReport(encounterScoop, new ArrayList<>());
    this.setAnswer(PIHCConstants.HOSPITALIZED_AND_VENTILATED, hospitalizedAndVentilatedReport.getReportCount());

    // ED/Overflow
    EdOverflowReport edOverflowReport = new EdOverflowReport(encounterScoop, new ArrayList<>());
    this.setAnswer(PIHCConstants.ED_OVERFLOW, edOverflowReport.getReportCount());

    // ED/Overflow and Ventilated
    EdOverflowAndVentilatedReport edOverflowAndVentilatedReport = new EdOverflowAndVentilatedReport(encounterScoop, new ArrayList<>());
    this.setAnswer(PIHCConstants.ED_OVERFLOW_AND_VENTILATED, edOverflowAndVentilatedReport.getReportCount());

    // Deaths
    DeathReport deathReport = new DeathReport(encounterScoop, new ArrayList<>(), java.sql.Date.valueOf(LocalDate.now().minusDays(1)));
    this.setAnswer(PIHCConstants.DEATHS, deathReport.getReportCount());

    // TODO: Hospital Onset

    // TODO: Previous Day Hospital Onset
  }
}
