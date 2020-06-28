package com.lantanagroup.nandina.query.fhir.r4.saner;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.query.fhir.r4.AbstractQuery;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSanerQuery extends AbstractQuery {

  public static final String MEASURE_GROUP_SYSTEM = "http://hl7.org/fhir/us/saner/CodeSystem/MeasureGroupSystem";
  public static final String MEASURE_POPULATION_SYSTEM = "http://hl7.org/fhir/us/saner/CodeSystem/MeasurePopulationSystem";
  public static final String MEASURE_URL = "http://hl7.org/fhir/us/saner/Measure/CDCPatientImpactAndHospitalCapacity";

  public AbstractSanerQuery(JsonProperties jsonProperties, IGenericClient fhirClient, HashMap<String, String> criteria) {
    super(jsonProperties, fhirClient, criteria);
    // TODO Auto-generated constructor stub
  }

  private static boolean isPopulationMatch(MeasureReport.MeasureReportGroupPopulationComponent population, String populationCode) {
    if (population.getCode() != null && population.getCode().getCoding() != null && population.getCode().getCoding().size() > 0) {
      if (population.getCode().getCoding().get(0).getSystem() != null && population.getCode().getCoding().get(0).getCode() != null) {
        return population.getCode().getCoding().get(0).getSystem().equals(MEASURE_POPULATION_SYSTEM) &&
                population.getCode().getCoding().get(0).getCode().equals(populationCode);
      }
    }

    return false;
  }

  private static boolean isGroupMatch(MeasureReport.MeasureReportGroupComponent group, String groupCode) {
    if (group.getCode() != null && group.getCode().getCoding().size() > 0) {
      if (group.getCode().getCoding().get(0).getSystem() != null && group.getCode().getCoding().get(0).getCode() != null) {
        return group.getCode().getCoding().get(0).getSystem().equals(MEASURE_GROUP_SYSTEM) &&
                group.getCode().getCoding().get(0).getCode().equals(groupCode);
      }
    }

    return false;
  }

  /**
   * If it is the first query executed (HospitalizedQuery) it retrieves MeasureReports from the FHIR server and
   * caches them. Otherwise, it gets the cached data from the HospitalizedQuery and returns that.
   *
   * @return
   */
  @Override
  protected Map<String, Resource> queryForData() {
    if (this.getClass().getSimpleName().equals("HospitalizedQuery")) {
      String reportDate = this.criteria.get("reportDate");
      String overflowLocations = this.criteria.get("overflowLocations");

      String url = String.format("MeasureReport?measure=%s&date=%s&",
              Helper.URLEncode(MEASURE_URL),
              Helper.URLEncode(reportDate));

      if (overflowLocations != null && !overflowLocations.isEmpty()) {
        url += String.format("subject=%s&", overflowLocations);
      }

      return this.search(url);
    } else {
      try {
        HospitalizedQuery hq = (HospitalizedQuery) this.getCachedQuery(jsonProperties.getQuery().get(JsonProperties.HOSPITALIZED));
        return hq.getData();
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
    }
  }

  protected Integer countForPopulation(Map<String, Resource> data, String groupCode, String populationCode) {
    Integer total = null;

    if (data != null) {
      for (Resource resource : data.values()) {
        MeasureReport mr = (MeasureReport) resource;

        for (MeasureReport.MeasureReportGroupComponent group : mr.getGroup()) {
          if (!isGroupMatch(group, groupCode)) continue;

          for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
            if (!isPopulationMatch(population, populationCode)) continue;

            if (population.getCountElement() != null && population.getCountElement().getValue() != null) {
              total = (total != null ? total : 0) + population.getCount();
            }
          }
        }
      }
    }

    return total;
  }
}
