package com.lantanagroup.nandina.fhir4.scoopfilterreport;

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
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Encounter.EncounterHospitalizationComponent;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class PillboxCsvReport {

	protected static final Logger logger = LoggerFactory.getLogger(PillboxCsvReport.class);
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	protected static FhirContext ctx = FhirContext.forR4();
	protected IValidationSupport validationSupport = (IValidationSupport) ctx.getValidationSupport();
	protected FHIRPathEngine fpe = new FHIRPathEngine(new HapiWorkerContext(ctx, validationSupport));
	
	protected String facilityId;
	protected String censusId;
	protected List<PatientData> covidPatientDataList;

	public PillboxCsvReport(String fhirBaseUrl, Scoop scoop) {
		Filter filter = new Filter();
		covidPatientDataList = new ArrayList<PatientData>();
		for (String key : scoop.getPatientMap().keySet()) {
			PatientData pd;
			try {
				Patient p = scoop.getPatientMap().get(key);
				pd = new PatientData(scoop, p);
				if (filter.isCovidPatient(pd)) {
					covidPatientDataList.add(pd);
				}
			} catch (Exception e) {
				logger.info("Error loading data for " + key, e);
			} 
		}
	}
	
	public String getUniqueCsv() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("DateCollected,Facility_ID,Census_ID,Patient_ID,Admit_Date,Discharge_Date,Patient_age,Patient_sex,Patient_race,Patient_ethnicity,Chief_complaint,Primary_dx,Patient_location,Disposition");
		for (PatientData pd : covidPatientDataList) {
			pw.print(this.getUniqueCsvRow(pd,facilityId, censusId));
		}
		pw.close();
		return sw.toString();
	}
	

	public String getMedsCsv() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("Patient_ID,Medication_Name,Medication_Code,Medication_Dose,Medication_Route,Medication_Start,Medication_Stop");
		for (PatientData pd : covidPatientDataList) {
			pw.print(this.getMedCsvRows(pd));
		}
		pw.close();
		return sw.toString();
	}
	
	public String getDxCsv() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("Patient_ID,Other_dx");
		for (PatientData pd : covidPatientDataList) {
			pw.print(this.getCsvDxRows(pd));
		}
		pw.close();
		return sw.toString();
	}
	
	public String getLabCsv() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("Patient_ID,SARSCOV_Lab_Order,SARSCOV_Lab_DateTime,SARSCOV_Lab_Result");
		for (PatientData pd : covidPatientDataList) {
			pw.print(this.getCsvLabRows(pd));
		}
		pw.close();
		return sw.toString();
	}
	
	public byte[] getZippedCSVs() throws IOException {
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
	
	public Bundle getBundle() {
		Bundle b = new Bundle();
		b.setType(BundleType.COLLECTION);
		for (PatientData pd : covidPatientDataList) {
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
		return sdf.format(pd.dateCollected);
	}
	
	public String getPatientId(PatientData pd) {
		String idStr = "";
		Identifier id = pd.patient.getIdentifierFirstRep();
		if (id != null) {
			idStr = id.getSystem() + "|" + id.getValue();
		}
		return idStr;
	}

	public String getAdmitDate(PatientData pd) {
		String dateStr = null;
		if (pd.encounter.hasPeriod()) {
			dateStr = sdf.format(pd.encounter.getPeriod().getStart());
		}
		return dateStr;
	}

	public String getDischargeDate(PatientData pd) {
		String dateStr = null;
		if (pd.encounter.hasPeriod()) {
			dateStr = sdf.format(pd.encounter.getPeriod().getEnd());
		}
		return dateStr;
	}

	public String getPatientAge(PatientData pd) {
		String patientAge = null;
		if (pd.patient.getBirthDate() != null) {
			Calendar bd = Calendar.getInstance();
			bd.setTime(pd.patient.getBirthDate());
			LocalDate localBd = LocalDate.of(bd.get(Calendar.YEAR), bd.get(Calendar.MONTH), bd.get(Calendar.DAY_OF_MONTH) );
			Period p = Period.between(localBd, LocalDate.now());
			patientAge = "" + p.getYears();
		}
		return patientAge;
	}

	public String getPatientSex(PatientData pd) {
		String sex = null;
		if (pd.patient.hasGender()) {
			sex = pd.patient.getGender().toCode();
		}
		return sex;
	}

	public String getPatientRace(PatientData pd) {
		String value = null;
		Extension race = pd.patient.getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
		if (race != null) {
			Extension omb = race.getExtensionByUrl("ombCategory");
			Coding c = (Coding) omb.getValue();
			value = c.getSystem() + "|" + c.getCode();
			
		}
		return value;
	}

	public String getPatientEthnicity(PatientData pd) {
		String value = null;
		Extension ethnicity = pd.patient.getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");
		if (ethnicity != null) {
			Extension omb = ethnicity.getExtensionByUrl("ombCategory");
			Coding c = (Coding) omb.getValue();
			value = c.getSystem() + "|" + c.getCode();
			
		}
		return value;
	}
	
	private String getPatientDisposition(PatientData pd) {
		String value = null;
		if (pd.encounter != null && pd.encounter.getHospitalization() != null) {
			EncounterHospitalizationComponent hosp = pd.encounter.getHospitalization();
			if (hosp.getDischargeDisposition() != null) {
				CodeableConcept cc = hosp.getDischargeDisposition();
				value = cc.getCodingFirstRep().getSystem() + "|" + cc.getCodingFirstRep().getCode();
			}
		}
		return value;
	}

	private String getPatientLocation(PatientData pd) {
		String value = null;
		if (pd.encounter != null && pd.encounter.getLocationFirstRep() != null) {
			Location loc = pd.encounter.getLocationFirstRep().getLocationTarget();
			value = loc.getName();
		}
		return value;
	}

	private String getPatientPrimaryDx(PatientData pd) {
		String value = null;
		if (pd.primaryDx != null) {
			value = pd.primaryDx.getCodingFirstRep().getSystem() + "|" + pd.primaryDx.getCodingFirstRep().getCode();
		}
		return value;
	}

	private String getPatientChiefComplaint(PatientData pd) {
		String value = null;
		if (pd.encounter != null && pd.encounter.getReasonCodeFirstRep() != null) {
			value = pd.encounter.getReasonCodeFirstRep().getCodingFirstRep().getSystem() + "|" + pd.encounter.getReasonCodeFirstRep().getCodingFirstRep().getCode();
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
			DateTimeType date = (DateTimeType)result.get(0);
			value = date.asStringValue();
		}
		return value;
	}

	private String getMedStart(MedicationRequest med) {
		String value = null;
		List<Base> result = fpe.evaluate(med, "dosageInstruction.timing.repeat.bounds.ofType(Period).start");
		if (result.size() > 0) {
			logger.info("Found med start");
			DateTimeType date = (DateTimeType)result.get(0);
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
			CodeableConcept cc = (CodeableConcept)route.get(0);
			value = this.codeableConceptToString(cc);
		}
		return value;
	}

	private String getMedDose(MedicationRequest med) {
		String value = null;
		List<Base> dose = fpe.evaluate(med, "dosageInstruction.doseAndRate");
		if (dose != null && dose.size() > 0) {
			Dosage.DosageDoseAndRateComponent doseAndRate = (Dosage.DosageDoseAndRateComponent)dose.get(0);
			value = quantityToString(doseAndRate.getDoseQuantity());
			logger.info("Found doseAndRate");
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
		for (BundleEntryComponent entry : pd.meds.getEntry() ) {
			MedicationRequest med = (MedicationRequest)entry.getResource();
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
		for (BundleEntryComponent entry : pd.conditions.getEntry() ) {
			Condition c = (Condition)entry.getResource();
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
		for (BundleEntryComponent entry : pd.labResults.getEntry() ) {
			Observation obs = (Observation)entry.getResource();
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
	

}
