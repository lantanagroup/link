package com.lantanagroup.nandina.query.pihc.fhir.r4.cerner;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.model.DiagnosisReportModel;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.model.LabReportModel;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.model.MedReportModel;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.model.PatientReportModel;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.report.*;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Encounter.EncounterHospitalizationComponent;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PillboxCsvReport extends Report {

  protected static FhirContext ctx = FhirContext.forR4();
  protected IValidationSupport validationSupport = (IValidationSupport) ctx.getValidationSupport();
  protected FHIRPathEngine fpe = new FHIRPathEngine(new HapiWorkerContext(ctx, validationSupport));
  protected String facilityId;
  protected String censusId;
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

  public PillboxCsvReport(FhirContext ctx) {
    super(null, new ArrayList<>(), ctx);
  }

  public PillboxCsvReport(EncounterScoop scoop, List<Filter> filters, FhirContext ctx) {
    super(scoop, addFilters(filters), ctx);
  }

  private static List<Filter> addFilters(List<Filter> filters) {
    Filter filter = new CovidFilter();
    filters.add(filter);
    return filters;
  }

  public String getUniqueCsv() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("DateCollected,Facility_ID,Census_ID,Patient_ID,Admit_Date,Discharge_Date,Patient_age,Patient_sex,Patient_race,Patient_ethnicity,Chief_complaint,Primary_dx,Patient_location,Disposition");
    for (PatientData pd : patientData) {
      pw.print(this.getUniqueCsvRow(pd, facilityId, censusId));
    }
    pw.close();
    return sw.toString();
  }

  public List<PatientReportModel> getUniqueData() {
    List<PatientReportModel> patientReports = this.patientData.stream().map(pd -> {
      PatientReportModel prm = new PatientReportModel();
      prm.setFacilityId(this.facilityId);
      prm.setCensusId(this.censusId);
      prm.setPatientId(this.getPatientId(pd));
      prm.setAdmitDate(this.getAdmitDate(pd));
      prm.setDischargeDate(this.getDischargeDate(pd));
      prm.setAge(this.getPatientAge(pd));
      prm.setSex(this.getPatientSex(pd));
      prm.setRace(this.getPatientRace(pd));
      prm.setEthnicity(this.getPatientEthnicity(pd));
      prm.setChiefComplaint(this.getPatientChiefComplaint(pd));
      prm.setPrimaryDx(this.getPatientPrimaryDx(pd));
      prm.setLocation(this.getPatientLocation(pd));
      prm.setDisposition(this.getPatientDisposition(pd));
      return prm;
    }).collect(Collectors.toList());
    return patientReports;
  }

  public String getMedsCsv() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Patient_ID,Medication_Name,Medication_Code,Medication_Dose,Medication_Route,Medication_Start,Medication_Stop");
    for (PatientData pd : patientData) {
      pw.print(this.getMedCsvRows(pd));
    }
    pw.close();
    return sw.toString();
  }

  public List<MedReportModel> getMedsData() {
    List<MedReportModel> medsData = new ArrayList<>();
    for (PatientData pd : this.patientData) {
      for (BundleEntryComponent medEntry : pd.getMeds().getEntry()) {
        MedicationRequest med = (MedicationRequest) medEntry.getResource();

        MedReportModel mrm = new MedReportModel();
        mrm.setPatientId(this.getPatientId(pd));
        mrm.setName(this.getMedName(med));
        mrm.setCode(this.getMedCode(med));
        mrm.setDose(this.getMedDose(med));
        mrm.setRoute(this.getMedRoute(med));
        mrm.setStart(this.getMedStart(med));
        mrm.setEnd(this.getMedEnd(med));

        medsData.add(mrm);
      }
    }
    return medsData;
  }

  public String getDxCsv() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Patient_ID,Other_dx");
    for (PatientData pd : patientData) {
      pw.print(this.getCsvDxRows(pd));
    }
    pw.close();
    return sw.toString();
  }

  public List<DiagnosisReportModel> getDxData() {
    List<DiagnosisReportModel> dxData = new ArrayList<>();
    for (PatientData pd : this.patientData) {
      for (BundleEntryComponent dxEntry : pd.getConditions().getEntry()) {
        Condition condition = (Condition) dxEntry.getResource();

        DiagnosisReportModel drm = new DiagnosisReportModel();
        drm.setPatientId(this.getPatientId(pd));
        drm.setCode(this.getDxCode(condition));

        dxData.add(drm);
      }
    }

    return dxData;
  }

  public String getLabCsv() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Patient_ID,SARSCOV_Lab_Order,SARSCOV_Lab_DateTime,SARSCOV_Lab_Result");
    for (PatientData pd : patientData) {
      pw.print(this.getCsvLabRows(pd));
    }
    pw.close();
    return sw.toString();
  }

  public Bundle getReportBundle() {
    Bundle b = new Bundle();
    b.setType(BundleType.COLLECTION);
    for (PatientData pd : patientData) {
      b.addEntry().setResource(pd.getBundle());
    }
    return b;
  }

  public String getFacilityId() {
    return facilityId;
  }

  public void setFacilityId(String facilityId) {
    this.facilityId = facilityId;
  }

  public String getCensusId() {
    return censusId;
  }

  public void setCensusId(String censusId) {
    this.censusId = censusId;
  }


  public String getDateCollectedString(PatientData pd) {
    return sdf.format(pd.getDateCollected());
  }

  public String getPatientId(PatientData pd) {
    String idStr = "";
    Identifier id = pd.getPatient().getIdentifierFirstRep();
    if (id != null) {
      idStr = id.getSystem() + "|" + id.getValue();
    }
    return idStr;
  }

  public String getAdmitDate(PatientData pd) {
    String dateStr = null;
    if (pd.getPrimaryEncounter().hasPeriod()) {
      dateStr = sdf.format(pd.getPrimaryEncounter().getPeriod().getStart());
    }
    return dateStr;
  }

  public String getDischargeDate(PatientData pd) {
    String dateStr = null;
    if (pd.getPrimaryEncounter().hasPeriod() && pd.getPrimaryEncounter().getPeriod().hasEnd()) {
      dateStr = sdf.format(pd.getPrimaryEncounter().getPeriod().getEnd());
    }
    return dateStr;
  }

  public String getPatientAge(PatientData pd) {
    String patientAge = null;
    if (pd.getPatient().getBirthDate() != null) {
      Calendar bd = Calendar.getInstance();
      bd.setTime(pd.getPatient().getBirthDate());
      LocalDate localBd = LocalDate.of(bd.get(Calendar.YEAR), bd.get(Calendar.MONTH) + 1, bd.get(Calendar.DAY_OF_MONTH) + 1);
      Period p = Period.between(localBd, LocalDate.now());
      patientAge = "" + p.getYears();
    }
    return patientAge;
  }

  public String getPatientSex(PatientData pd) {
    String sex = null;
    if (pd.getPatient().hasGender()) {
      sex = pd.getPatient().getGender().toCode();
    }
    return sex;
  }

  public String getPatientRace(PatientData pd) {
    String value = null;
    Extension race = pd.getPatient().getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");

    if (race != null) {
      Extension omb = race.getExtensionByUrl("ombCategory");

      if (omb != null) {
        Coding c = (Coding) omb.getValue();
        value = c.getSystem() + "|" + c.getCode();
      }
    }
    return value;
  }

  public String getPatientEthnicity(PatientData pd) {
    String value = null;
    Extension ethnicity = pd.getPatient().getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");

    if (ethnicity != null) {
      Extension omb = ethnicity.getExtensionByUrl("ombCategory");

      if (omb != null && omb.getValue() != null) {
        Coding c = (Coding) omb.getValue();
        value = c.getSystem() + "|" + c.getCode();
      }
    }

    return value;
  }

  private String getPatientDisposition(PatientData pd) {
    String value = null;
    if (pd.getPrimaryEncounter() != null && pd.getPrimaryEncounter().getHospitalization() != null) {
      EncounterHospitalizationComponent hosp = pd.getPrimaryEncounter().getHospitalization();
      if (hosp.getDischargeDisposition() != null) {
        CodeableConcept cc = hosp.getDischargeDisposition();
        value = cc.getCodingFirstRep().getSystem() + "|" + cc.getCodingFirstRep().getCode();
      }
    }
    return value;
  }

  private String getPatientLocation(PatientData pd) {
    String value = null;
    if (pd.getPrimaryEncounter() != null && pd.getPrimaryEncounter().getLocationFirstRep() != null) {
      Location loc = pd.getPrimaryEncounter().getLocationFirstRep().getLocationTarget();
      value = loc.getName();
    }
    return value;
  }

  private String getPatientPrimaryDx(PatientData pd) {
    String value = null;
    if (pd.getPrimaryDx() != null) {
      value = pd.getPrimaryDx().getCodingFirstRep().getSystem() + "|" + pd.getPrimaryDx().getCodingFirstRep().getCode();
    }
    return value;
  }

  private String getPatientChiefComplaint(PatientData pd) {
    String value = null;
    if (pd.getPrimaryEncounter() != null && pd.getPrimaryEncounter().getReasonCodeFirstRep() != null) {
      value = pd.getPrimaryEncounter().getReasonCodeFirstRep().getCodingFirstRep().getSystem() + "|" + pd.getPrimaryEncounter().getReasonCodeFirstRep().getCodingFirstRep().getCode();
    }
    return value;
  }


  private String getDxCode(Condition c) {
    return codeableConceptToString(c.getCode());
  }


  private String getLabOrder(Observation obs) {
    return codeableConceptToString(obs.getCode());
  }

  private String getLabDateTime(Observation obs) {
    return obs.getEffectiveDateTimeType().asStringValue();
  }

  private String getLabResult(Observation obs) {
    String value = null;
    if (obs.hasValueCodeableConcept()) {
      value = codeableConceptToString(obs.getValueCodeableConcept());
    } else if (obs.hasValueQuantity()) {
      value = this.quantityToString(obs.getValueQuantity());
    }
    return value;
  }

  private String codeableConceptToString(CodeableConcept cc) {
    return cc.getCodingFirstRep().getSystem() + "|" + cc.getCodingFirstRep().getCode();
  }


  private String getMedEnd(MedicationRequest med) {
    String value = null;
    List<Base> result = fpe.evaluate(med, "dosageInstruction.timing.repeat.bounds.ofType(Period).end");
    if (result.size() > 0) {
      DateTimeType date = (DateTimeType) result.get(0);
      value = date.asStringValue();
    }
    return value;
  }

  private String getMedStart(MedicationRequest med) {
    String value = null;
    List<Base> result = fpe.evaluate(med, "dosageInstruction.timing.repeat.bounds.ofType(Period).start");
    if (result.size() > 0) {
      //	logger.info("Found med start");
      DateTimeType date = (DateTimeType) result.get(0);
      value = date.asStringValue();
    } else if (
            med.hasDosageInstruction()
                    && med.getDosageInstructionFirstRep().hasTiming()
                    && med.getDosageInstructionFirstRep().getTiming().hasEvent()) {

      List<DateTimeType> dateList = med.getDosageInstructionFirstRep().getTiming().getEvent();
      DateTimeType date = dateList.get(0);
      value = date.asStringValue();
    }

    return value;
  }

  private String getMedRoute(MedicationRequest med) {
    String value = null;
    List<Base> route = fpe.evaluate(med, "dosageInstruction.route");
    if (route.size() > 0) {
      CodeableConcept cc = (CodeableConcept) route.get(0);
      value = this.codeableConceptToString(cc);
    }
    return value;
  }

  private String getMedDose(MedicationRequest med) {
    String value = null;
    List<Base> dose = fpe.evaluate(med, "dosageInstruction.doseAndRate");
    if (dose != null && dose.size() > 0) {
      Dosage.DosageDoseAndRateComponent doseAndRate = (Dosage.DosageDoseAndRateComponent) dose.get(0);
      value = quantityToString(doseAndRate.getDoseQuantity());
      //	logger.info("Found doseAndRate");
    }
    return value;
  }

  private String quantityToString(Quantity quantity) {
    String value = null;
    if (quantity.hasValue()) {
      value = quantity.getValue() + " " + quantity.getUnit();
    } else if (quantity.hasDisplay()) {
      value = quantity.getDisplay();
    }
    return value;
  }

  private String getMedCode(MedicationRequest med) {
    String value = null;
    if (med.hasMedicationCodeableConcept() && med.getMedicationCodeableConcept().hasCoding()) {
      value = codeableConceptToString(med.getMedicationCodeableConcept());
    } else if (med.hasMedication()) {
      logger.info("MedicationRequest.medicationReference not yet supported");
    }
    return value;
  }

  private String getMedName(MedicationRequest med) {
    String value = null;
    if (med.hasMedicationCodeableConcept() && med.getMedicationCodeableConcept().hasText()) {
      value = med.getMedicationCodeableConcept().getText();
    } else if (med.hasMedication()) {
      logger.info("MedicationRequest.medicationReference not yet supported");
    }
    return value;
  }

  public String getUniqueCsvRow(PatientData pd, String facilityId, String censusId) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println(
            getDateCollectedString(pd)
                    + "," + facilityId
                    + "," + censusId
                    + "," + getPatientId(pd)
                    + "," + getAdmitDate(pd)
                    + "," + getDischargeDate(pd)
                    + "," + getPatientAge(pd)
                    + "," + getPatientSex(pd)
                    + "," + getPatientRace(pd)
                    + "," + getPatientEthnicity(pd)
                    + "," + getPatientChiefComplaint(pd)
                    + "," + getPatientPrimaryDx(pd)
                    + "," + getPatientLocation(pd)
                    + "," + getPatientDisposition(pd)
    );
    pw.close();
    return sw.toString();
  }

  public String getMedCsvRows(PatientData pd) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    for (BundleEntryComponent entry : pd.getMeds().getEntry()) {
      MedicationRequest med = (MedicationRequest) entry.getResource();
      pw.println(
              getPatientId(pd)
                      + "," + getMedName(med)
                      + "," + getMedCode(med)
                      + "," + getMedDose(med)
                      + "," + getMedRoute(med)
                      + "," + getMedStart(med)
                      + "," + getMedEnd(med)

      );
    }

    pw.close();
    return sw.toString();
  }


  public String getCsvDxRows(PatientData pd) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    for (BundleEntryComponent entry : pd.getConditions().getEntry()) {
      Condition c = (Condition) entry.getResource();
      pw.println(
              getPatientId(pd)
                      + "," + getDxCode(c)

      );
    }

    pw.close();
    return sw.toString();
  }

  public String getCsvLabRows(PatientData pd) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    for (BundleEntryComponent entry : pd.getLabResults().getEntry()) {
      Observation obs = (Observation) entry.getResource();
      logger.info(obs.getId());
      pw.println(
              getPatientId(pd)
                      + "," + getLabOrder(obs)
                      + "," + getLabDateTime(obs)
                      + "," + getLabResult(obs)

      );
    }

    pw.close();
    return sw.toString();
  }

  public List<LabReportModel> getLabData() {
    List<LabReportModel> labData = new ArrayList<>();
    for (PatientData pd : this.patientData) {
      for (BundleEntryComponent labEntry : pd.getLabResults().getEntry()) {
        Observation obs = (Observation) labEntry.getResource();

        LabReportModel lrm = new LabReportModel();
        lrm.setPatientId(this.getPatientId(pd));
        lrm.setOrder(this.getLabOrder(obs));
        lrm.setDate(this.getLabDateTime(obs));
        lrm.setResult(this.getLabResult(obs));

        labData.add(lrm);
      }
    }

    return labData;
  }

  public byte[] getReportData() throws IOException {
    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(bs);
    zos.putNextEntry(new ZipEntry("unique.csv"));
    zos.write(this.getUniqueCsv().getBytes());
    zos.closeEntry();
    zos.putNextEntry(new ZipEntry("meds.csv"));
    zos.write(this.getMedsCsv().getBytes());
    zos.closeEntry();
    zos.putNextEntry(new ZipEntry("dx.csv"));
    zos.write(this.getDxCsv().getBytes());
    zos.closeEntry();
    zos.putNextEntry(new ZipEntry("lab.csv"));
    zos.write(this.getLabCsv().getBytes());
    zos.closeEntry();
    zos.close();
    return bs.toByteArray();
  }

}
