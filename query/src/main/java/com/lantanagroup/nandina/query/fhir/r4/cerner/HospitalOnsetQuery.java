package com.lantanagroup.nandina.query.fhir.r4.cerner;

import com.lantanagroup.nandina.query.BaseQuery;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HospitalOnsetQuery extends BaseQuery {
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
            this.addContextData("hospitalOnset", this);
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
                    hospitalOnset = onsetDuringEncounter(condition, encounter);
                }
            }
        }
        return hospitalOnset;
    }

    private boolean onsetDuringEncounter(Condition condition, Encounter encounter) {
        boolean hospitalOnset = false;
        Period period = encounter.getPeriod();
        if (period != null) {

            Date encounterStartDate = encounter.getPeriod().getStart();
            Date encounterEndDate = encounter.getPeriod().getEnd();

            if (condition.hasOnsetDateTimeType()) {
                Calendar onsetDate = condition.getOnsetDateTimeType().toCalendar();
                hospitalOnset = onsetDuringEncounter(onsetDate, encounterStartDate, encounterEndDate);
            } else if (condition.hasOnsetPeriod()) {
                Calendar onsetPeriodStart = condition.getOnsetPeriod().getStartElement().toCalendar();
                hospitalOnset = onsetDuringEncounter(onsetPeriodStart, encounterStartDate, encounterEndDate);
                if (hospitalOnset == false) {
                    Calendar onsetPeriodEnd = condition.getOnsetPeriod().getEndElement().toCalendar();
                    hospitalOnset = onsetDuringEncounter(onsetPeriodEnd, encounterStartDate, encounterEndDate);
                }
            }
        }
        // TODO: at some point maybe try to deal with Condition.onsetRange if the UCUM unit is something time related, but would needs to see some real world sample data that shows this is necessary.
        return hospitalOnset;
    }

    private boolean onsetDuringEncounter(Calendar onsetDate, Date encounterStartDate, Date encounterEndDate) {
        boolean hospitalOnset = false;
        if (encounterStartDate != null) {
            Calendar encStartPlus14 = Calendar.getInstance();
            encStartPlus14.setTime(encounterStartDate);
            encStartPlus14.roll(Calendar.DAY_OF_YEAR, 14);
            if (onsetDate.after(encStartPlus14)) {
                hospitalOnset = true;
                if (encounterEndDate != null) {
                    Calendar encEnd = Calendar.getInstance();
                    encEnd.setTime(encounterEndDate);
                    if (onsetDate.after(encEnd)) {
                        // onset after discharge
                        hospitalOnset = false;
                    }

                }
            }
        }
        return hospitalOnset;
    }
}