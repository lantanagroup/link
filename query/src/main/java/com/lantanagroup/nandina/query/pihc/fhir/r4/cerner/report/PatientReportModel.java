package com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.report;

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

  public String getDisposition() {
    return disposition;
  }

  public void setDisposition(String disposition) {
    this.disposition = disposition;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getPrimaryDx() {
    return primaryDx;
  }

  public void setPrimaryDx(String primaryDx) {
    this.primaryDx = primaryDx;
  }

  public String getChiefComplaint() {
    return chiefComplaint;
  }

  public void setChiefComplaint(String chiefComplaint) {
    this.chiefComplaint = chiefComplaint;
  }

  public String getEthnicity() {
    return ethnicity;
  }

  public void setEthnicity(String ethnicity) {
    this.ethnicity = ethnicity;
  }

  public String getRace() {
    return race;
  }

  public void setRace(String race) {
    this.race = race;
  }

  public String getSex() {
    return sex;
  }

  public void setSex(String sex) {
    this.sex = sex;
  }

  public String getAge() {
    return age;
  }

  public void setAge(String age) {
    this.age = age;
  }

  public String getDischargeDate() {
    return dischargeDate;
  }

  public void setDischargeDate(String dischargeDate) {
    this.dischargeDate = dischargeDate;
  }

  public String getAdmitDate() {
    return admitDate;
  }

  public void setAdmitDate(String admitDate) {
    this.admitDate = admitDate;
  }

  public String getPatientId() {
    return patientId;
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }

  public String getCensusId() {
    return censusId;
  }

  public void setCensusId(String censusId) {
    this.censusId = censusId;
  }

  public String getFacilityId() {
    return facilityId;
  }

  public void setFacilityId(String facilityId) {
    this.facilityId = facilityId;
  }
}
