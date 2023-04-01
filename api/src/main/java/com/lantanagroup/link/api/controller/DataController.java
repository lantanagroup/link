package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.config.datagovernance.DataGovernanceConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.MeasureDefinition;
import com.lantanagroup.link.db.model.PatientData;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.model.TestResponse;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import com.lantanagroup.link.query.uscore.PatientScoop;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
public class DataController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(DataController.class);

  @Autowired
  @Setter
  @Getter
  private DataGovernanceConfig dataGovernanceConfig;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private USCoreConfig usCoreConfig;

  @Autowired
  private MongoService mongoService;

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }

  private static boolean shouldDelete(String retentionPeriod, Date lastDatePosted) throws DatatypeConfigurationException {
    Date comp = adjustTime(retentionPeriod, lastDatePosted);
    Date today = new Date();
    return today.compareTo(comp) >= 0;
  }

  /**
   * @param retentionPeriod how long to keep the data
   * @param lastDatePosted  when the data was last updated
   * @return the adjusted day from when the data had been last updated to the end of its specified retention period
   * @throws DatatypeConfigurationException
   */
  private static Date adjustTime(String retentionPeriod, Date lastDatePosted) throws DatatypeConfigurationException {
    Duration dur = DatatypeFactory.newInstance().newDuration(retentionPeriod);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(lastDatePosted);

    calendar.add(Calendar.YEAR, dur.getYears());
    calendar.add(Calendar.MONTH, dur.getMonths());
    calendar.add(Calendar.DAY_OF_MONTH, dur.getDays());
    calendar.add(Calendar.HOUR_OF_DAY, dur.getHours());
    calendar.add(Calendar.MINUTE, dur.getMinutes());
    calendar.add(Calendar.SECOND, dur.getSeconds());

    return calendar.getTime();
  }

  /**
   * @throws DatatypeConfigurationException Deletes all census lists, patient data bundles, and measure reports stored on the server if their retention period
   *                                        has been reached.
   */
  @DeleteMapping(value = "/expunge")
  public Integer expungeData() throws DatatypeConfigurationException {
    if (this.dataGovernanceConfig == null) {
      logger.error("Data governance not configured");
      return 0;
    }

    List<PatientData> allPatientData = this.mongoService.getAllPatientData();
    List<String> patientDataToDelete = allPatientData.stream().filter(pd -> {
              try {
                return shouldDelete(this.dataGovernanceConfig.getPatientDataRetention(), pd.getRetrieved());
              } catch (DatatypeConfigurationException e) {
                return false;
              }
            })
            .map(pd -> pd.getId())
            .collect(Collectors.toList());

    if (patientDataToDelete.size() > 0) {
      this.mongoService.deletePatientData(patientDataToDelete);
    }

    return patientDataToDelete.size();
  }

  /**
   * @return
   */
  @GetMapping("/$test")
  public TestResponse test(@RequestParam String patientId, @RequestParam String measureId, @RequestParam String periodStart, @RequestParam String periodEnd) {
    TestResponse testResponse = new TestResponse();

    try {
      // Get the data
      logger.info("Testing querying/scooping data for the patient {}", patientId);
      QueryConfig queryConfig = this.applicationContext.getBean(QueryConfig.class);
      IQuery query = QueryFactory.getQueryInstance(this.applicationContext, queryConfig.getQueryClass());

      MeasureDefinition measureDef = this.mongoService.findMeasureDefinition(measureId);
      List<String> resourceTypes = FhirHelper.getDataRequirementTypes(measureDef.getBundle())
              .stream()
              .collect(Collectors.toList());
      resourceTypes.retainAll(this.usCoreConfig.getPatientResourceTypes());

      PatientScoop patientScoop = this.applicationContext.getBean(PatientScoop.class);
      patientScoop.setFhirQueryServer(query.getFhirQueryClient());

      PatientOfInterestModel poi = new PatientOfInterestModel();
      poi.setReference(patientId);

      ReportCriteria criteria = new ReportCriteria(List.of(measureId), periodStart, periodEnd);

      patientScoop.loadPatientData(criteria, new ReportContext(), List.of(poi), resourceTypes, List.of(measureId));
    } catch (Exception ex) {
      testResponse.setSuccess(false);
      testResponse.setMessage(ex.getMessage());
    }

    return testResponse;
  }
}
