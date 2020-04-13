package com.lantanagroup.flintlock.model;

public class QuestionnaireResponseSimple {
  private String facilityId;
  private String summaryCensusId;
  private String date;

  private Integer hospitalized;
  private Integer hospitalizedAndVentilated;
  private Integer hospitalOnset;
  private Integer edOverflow;
  private Integer edOverflowAndVentilated;
  private Integer deaths;

  private Integer allHospitalBeds;
  private Integer hospitalInpatientBeds;
  private Integer hospitalInpatientBedOccupancy;
  private Integer icuBeds;
  private Integer icuBedOccupancy;
  private Integer mechanicalVentilators;
  private Integer mechanicalVentilatorsInUse;

  public String getFacilityId() {
    return facilityId;
  }

  public void setFacilityId(String facilityId) {
    this.facilityId = facilityId;
  }

  public String getSummaryCensusId() {
    return summaryCensusId;
  }

  public void setSummaryCensusId(String summaryCensusId) {
    this.summaryCensusId = summaryCensusId;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public Integer getHospitalized() {
    return hospitalized;
  }

  public void setHospitalized(Integer hospitalized) {
    this.hospitalized = hospitalized;
  }

  public Integer getHospitalizedAndVentilated() {
    return hospitalizedAndVentilated;
  }

  public void setHospitalizedAndVentilated(Integer hospitalizedAndVentilated) {
    this.hospitalizedAndVentilated = hospitalizedAndVentilated;
  }

  public Integer getHospitalOnset() {
    return hospitalOnset;
  }

  public void setHospitalOnset(Integer hospitalOnset) {
    this.hospitalOnset = hospitalOnset;
  }

  public Integer getEdOverflow() {
    return edOverflow;
  }

  public void setEdOverflow(Integer edOverflow) {
    this.edOverflow = edOverflow;
  }

  public Integer getEdOverflowAndVentilated() {
    return edOverflowAndVentilated;
  }

  public void setEdOverflowAndVentilated(Integer edOverflowAndVentilated) {
    this.edOverflowAndVentilated = edOverflowAndVentilated;
  }

  public Integer getDeaths() {
    return deaths;
  }

  public void setDeaths(Integer deaths) {
    this.deaths = deaths;
  }

  public Integer getAllHospitalBeds() {
    return allHospitalBeds;
  }

  public void setAllHospitalBeds(Integer allHospitalBeds) {
    this.allHospitalBeds = allHospitalBeds;
  }

  public Integer getHospitalInpatientBeds() {
    return hospitalInpatientBeds;
  }

  public void setHospitalInpatientBeds(Integer hospitalInpatientBeds) {
    this.hospitalInpatientBeds = hospitalInpatientBeds;
  }

  public Integer getHospitalInpatientBedOccupancy() {
    return hospitalInpatientBedOccupancy;
  }

  public void setHospitalInpatientBedOccupancy(Integer hospitalInpatientBedOccupancy) {
    this.hospitalInpatientBedOccupancy = hospitalInpatientBedOccupancy;
  }

  public Integer getIcuBeds() {
    return icuBeds;
  }

  public void setIcuBeds(Integer icuBeds) {
    this.icuBeds = icuBeds;
  }

  public Integer getIcuBedOccupancy() {
    return icuBedOccupancy;
  }

  public void setIcuBedOccupancy(Integer icuBedOccupancy) {
    this.icuBedOccupancy = icuBedOccupancy;
  }

  public Integer getMechanicalVentilators() {
    return mechanicalVentilators;
  }

  public void setMechanicalVentilators(Integer mechanicalVentilators) {
    this.mechanicalVentilators = mechanicalVentilators;
  }

  public Integer getMechanicalVentilatorsInUse() {
    return mechanicalVentilatorsInUse;
  }

  public void setMechanicalVentilatorsInUse(Integer mechanicalVentilatorsInUse) {
    this.mechanicalVentilatorsInUse = mechanicalVentilatorsInUse;
  }
}
