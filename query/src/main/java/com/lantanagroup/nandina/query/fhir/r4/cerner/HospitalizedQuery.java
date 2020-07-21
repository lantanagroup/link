package com.lantanagroup.nandina.query.fhir.r4.cerner;

import com.lantanagroup.nandina.query.BaseQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HospitalizedQuery extends BaseQuery {
    @Override
    public Integer execute() {
        if (!this.criteria.containsKey("reportDate")) return null;

        Map<String, Resource> resMap = this.getData();

        this.addContextData("hospitalized", this);

        return this.getCount(resMap);
    }

    /**
     * Queries for Patient resources where
     * - the Patient is in referenced in Condition.patient and Condition.code is a code from the Covid19 value set
     * - the Patient is referenced in Encounter.patient and Encounter.class is one of IMP,ACUTE,NONAC,OBSENC
     * The result is then further filtered to just those where the Encounter.date is equal to the reportDate
     * (Encounter.date search parameter is not working properly, so this is done procedurally)
     */
    @Override
    protected Map<String, Resource> queryForData() {
        try {
            String reportDate = this.criteria.get("reportDate");
            EncounterScoop encounterScoop = (EncounterScoop) this.getContextData("scoopData");

            // Encounter.date search parameter not working with current release of HAPI, so
            // weeding out encounters outside the reportDate manually
            HashMap<String, Resource> finalPatientMap = filterPatientsByDate(reportDate, encounterScoop.getPatientMap());
            this.addContextData("hospitalized", finalPatientMap);

            logger.debug(String.format("Patients matching encounter date: %s", finalPatientMap.values().size()));

            return finalPatientMap;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}