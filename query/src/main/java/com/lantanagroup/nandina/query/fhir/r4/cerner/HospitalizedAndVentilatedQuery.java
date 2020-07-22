package com.lantanagroup.nandina.query.fhir.r4.cerner;

import com.lantanagroup.nandina.query.BaseQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.report.HospitalizedAndVentilatedReport;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HospitalizedAndVentilatedQuery extends BaseQuery {

    @Override
    public Integer execute() {
        EncounterScoop encounterScoop = (EncounterScoop) this.getContextData("scoopData");
        List<Filter> filters = new ArrayList<>();
        HospitalizedAndVentilatedReport hospitalizedAndVentilatedReport = new HospitalizedAndVentilatedReport(encounterScoop, filters);
        this.addContextData("HospitalizedAndVentilated", hospitalizedAndVentilatedReport.getReportCount());

        return hospitalizedAndVentilatedReport.getReportCount();
    }

    @Override
    protected Map<String, Resource> queryForData() {
        return null;
    }
}