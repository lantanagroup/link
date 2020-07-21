package com.lantanagroup.nandina.query.fhir.r4.cerner;

import com.lantanagroup.nandina.query.BaseQuery;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PreviousDayHospitalOnsetQuery extends BaseQuery {
    @Override
    public Integer execute() {
        if (!this.criteria.containsKey("reportDate") && !this.criteria.containsKey("overflowLocations")) {
            return null;
        }

        Map<String, Resource> resMap = this.getData();
        return this.getCount(resMap);
    }

    @Override
    protected Map<String, Resource> queryForData() {
        try {
            String reportDate = this.criteria.get("reportDate");
            String overflowLocations = this.criteria.get("overflowLocations");

            HospitalizedQuery hospitalizedQuery = (HospitalizedQuery) this.getContextData("hospitalized");
            Map<String, Resource> hqData = hospitalizedQuery.getData();
            HashMap<String, Resource> finalPatientMap = getHospitalOnsetPatients(reportDate, overflowLocations, hqData);
            this.addContextData("previousDayHospitalOnset", this);
            return finalPatientMap;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private HashMap<String, Resource> getHospitalOnsetPatients(String reportDate, String overflowLocations, Map<String, Resource> hospitalizedQueryData) {
        Set<String> patientIds = hospitalizedQueryData.keySet();
        HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
        for (String patientId : patientIds) {
            Patient patient = (Patient) hospitalizedQueryData.get(patientId);
            if (isHospitalOnset(patient)) {
                finalPatientMap.put(patientId, patient);
            }

        }
        return finalPatientMap;
    }

    private boolean isHospitalOnset(Patient p) {
        boolean hospitalOnset = false;
        Map<String, Resource> conditionMap = getPatientConditions(p);
        Map<String, Resource> encounterMap = getPatientEncounters(p);
        if (conditionMap != null && conditionMap.size() > 0) {
            for (String conditionId : conditionMap.keySet()) {
                Condition condition = (Condition) conditionMap.get(conditionId);
                for (String encounterId : encounterMap.keySet()) {
                    Encounter encounter = (Encounter) encounterMap.get(encounterId);
                    hospitalOnset = isOnsetDuringEncounter(condition, encounter);
                }
            }
        }
        return hospitalOnset;
    }

    private boolean isOnsetDuringEncounter(Condition condition, Encounter encounter) {
        boolean hospitalOnset = false;
        Period period = encounter.getPeriod();
        if (period != null) {
            Date encounterStartDate = encounter.getPeriod().getStart();
            Date encounterEndDate = encounter.getPeriod().getEnd();

            if (condition.hasOnsetDateTimeType()) {
                Calendar onsetDate = condition.getOnsetDateTimeType().toCalendar();
                hospitalOnset = isOnsetDuringEncounter(onsetDate, encounterStartDate, encounterEndDate);
            } else if (condition.hasOnsetPeriod()) {
                Calendar onsetPeriodStart = condition.getOnsetPeriod().getStartElement().toCalendar();
                hospitalOnset = isOnsetDuringEncounter(onsetPeriodStart, encounterStartDate, encounterEndDate);
            }
        }
        return hospitalOnset;
    }

    private boolean isOnsetDuringEncounter(Calendar onsetDate, Date encounterStartDate, Date encounterEndDate) {
        boolean hospitalOnset = false;
        if (encounterStartDate != null) {
            LocalDate encounterStartPlus14 = LocalDate.ofInstant(encounterStartDate.toInstant(), ZoneId.systemDefault()).plusDays(14);
            LocalDate previousDate = LocalDate.now().minusDays(1);
            if (previousDate.equals(encounterStartPlus14) &&
                    (onsetDate.equals(encounterStartPlus14) || onsetDate.after(encounterStartPlus14))) {
                hospitalOnset = true;
            }
        }
        return hospitalOnset;
    }
}