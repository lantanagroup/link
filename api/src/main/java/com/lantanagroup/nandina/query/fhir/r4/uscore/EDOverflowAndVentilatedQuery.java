package com.lantanagroup.nandina.query.fhir.r4.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.query.IQueryCountExecutor;
import com.lantanagroup.nandina.query.fhir.r4.AbstractQuery;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;

public class EDOverflowAndVentilatedQuery extends AbstractQuery implements IQueryCountExecutor {

  public EDOverflowAndVentilatedQuery(JsonProperties jsonProperties, IGenericClient fhirClient, HashMap<String, String> criteria) {
    super(jsonProperties, fhirClient, criteria);
    // TODO Auto-generated constructor stub
  }

  @Override
  public Integer execute() {
    Map<String, Resource> resMap = this.getData();
    return this.getCount(resMap);
  }

  /**
   * Takes the result of EDOverflowQuery.queryForData(), then further filters Patients where:
   * - The Patient is references in Device.patient and where Device.type is in the mechanical-ventilators value set
   */
  @Override
  protected Map<String, Resource> queryForData() {
    try {
      EDOverflowQuery query = (EDOverflowQuery) this.getCachedQuery(jsonProperties.getQuery().get(JsonProperties.ED_OVERFLOW));
      Map<String, Resource> queryData = query.getData();
      HashMap<String, Resource> finalPatientMap = ventilatedPatients(queryData);
      return finalPatientMap;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
