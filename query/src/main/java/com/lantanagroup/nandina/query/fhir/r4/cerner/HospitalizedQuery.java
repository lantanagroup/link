package com.lantanagroup.nandina.query.fhir.r4.cerner;

import com.lantanagroup.nandina.query.BaseQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.report.HospitalizedReport;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HospitalizedQuery extends BaseQuery {
    @Override
    public Integer execute() {
        if (!this.criteria.containsKey("reportDate")) return null;

        EncounterScoop encounterScoop = (EncounterScoop) this.getContextData("scoopData");
        List<Filter> filters = new ArrayList<Filter>();
        HospitalizedReport hospitalizedReport = new HospitalizedReport(encounterScoop, filters);
        this.addContextData("hospitalized", hospitalizedReport.getReportCount());

        return hospitalizedReport.getReportCount();
    }

    @Override
    protected Map<String, Resource> queryForData() {
        return null;
    }
}