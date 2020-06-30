package com.lantanagroup.nandina.query.r4.cerner.scoop;

import com.lantanagroup.nandina.query.r4.cerner.PatientData;

import java.util.Date;
import java.util.List;

public abstract class Scoop {

  protected List<PatientData> patientData;
  protected Date reportDate = null;

  public List<PatientData> getPatientData() {
    return patientData;
  }

  public Date getReportDate() {
    return reportDate;
  }

}
