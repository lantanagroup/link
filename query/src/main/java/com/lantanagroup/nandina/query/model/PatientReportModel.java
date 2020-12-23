package com.lantanagroup.nandina.query.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PatientReportModel {
  private String facilityId;
  private String censusId;
  private String patientId;
  private String admitDate;
  private String dischargeDate;
  private String age;
  private String sex;
  private String race;
  private String ethnicity;
  private String chiefComplaint;
  private String primaryDx;
  private String location;
  private String disposition;
}
