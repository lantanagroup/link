package com.lantanagroup.nandina.query.fhir.r4.uscore;

import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.query.fhir.r4.AbstractQuery;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;

public class HospitalizedAndVentilatedQuery extends AbstractQuery {

  @Override
  public Integer execute() {
    Map<String, Resource> resMap = this.getData();
    return this.getCount(resMap);
  }

  /**
   * Takes the result of HospitalizedQuery.queryForData(), then further filters Patients where:
   * - The Patient is references in Device.patient and where Device.type is in the mechanical-ventilators value set
   * - The patient is referenced in Procedure.patient and where Procedure.code is in the intubation codes
   */
  @Override
  protected Map<String, Resource> queryForData() {
    try {
      HospitalizedQuery hq = (HospitalizedQuery) this.getContextData("hospitalized");
      Map<String, Resource> hqData = hq.getData();
      HashMap<String, Resource> finalPatientMap = ventilatedPatients(hqData);
      return finalPatientMap;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
