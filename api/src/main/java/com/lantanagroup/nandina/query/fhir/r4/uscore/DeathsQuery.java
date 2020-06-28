package com.lantanagroup.nandina.query.fhir.r4.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import java.util.HashMap;
import java.util.Map;

import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.query.fhir.r4.AbstractQuery;
import com.lantanagroup.nandina.query.IQueryCountExecutor;
import org.hl7.fhir.r4.model.Resource;

public class DeathsQuery extends AbstractQuery implements IQueryCountExecutor {

  public DeathsQuery(JsonProperties jsonProperties, IGenericClient fhirClient, HashMap<String, String> criteria) {
    super(jsonProperties, fhirClient, criteria);
  }

  @Override
  public Integer execute() {
    if (!this.criteria.containsKey("reportDate")) return null;

    Map<String, Resource> resMap = this.getData();
    return this.getCount(resMap);
  }

  /**
   * Takes the result of HospitalizedQuery.queryForData(), then further filters Patients where:
   * - Patient.deceasedDateTime matches the reportDate parameter
   */
  @Override
  protected Map<String, Resource> queryForData() {
    try {
      String reportDate = this.criteria.get("reportDate");
      HospitalizedQuery hq = (HospitalizedQuery) this.getCachedQuery(jsonProperties.getQuery().get(JsonProperties.HOSPITALIZED));
      EDOverflowQuery eq = (EDOverflowQuery) this.getCachedQuery(jsonProperties.getQuery().get(JsonProperties.ED_OVERFLOW));
      Map<String, Resource> queryData = hq.getData();
      queryData.putAll(eq.getData());
      HashMap<String, Resource> finalPatientMap = deadPatients(queryData, reportDate);
      return finalPatientMap;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
