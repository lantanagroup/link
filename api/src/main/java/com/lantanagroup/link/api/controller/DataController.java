package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.AuditTypes;
import com.lantanagroup.link.db.model.PatientData;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.model.TestResponse;
import com.lantanagroup.link.query.uscore.PatientScoop;
import com.lantanagroup.link.time.StopwatchManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{tenantId}/data")
public class DataController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(DataController.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private SharedService sharedService;

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
  @DeleteMapping(value = "/$expunge")
  public Integer expungeData(@PathVariable String tenantId, @AuthenticationPrincipal LinkCredentials user, HttpServletRequest request) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    assert tenantService != null : "Tenant not instantiated";

    if (StringUtils.isEmpty(tenantService.getConfig().getRetentionPeriod())) {
      logger.error("Tenant retention period is not configured");
      return 0;
    }

    List<PatientData> allPatientData = tenantService.getAllPatientData();
    List<String> patientDataToDelete = allPatientData.stream().filter(pd -> {
              try {
                return shouldDelete(tenantService.getConfig().getRetentionPeriod(), pd.getRetrieved());
              } catch (DatatypeConfigurationException e) {
                return false;
              }
            })
            .map(PatientData::getId)
            .collect(Collectors.toList());

    if (patientDataToDelete.size() > 0) {
      tenantService.deletePatientData(patientDataToDelete);

      logger.info("Deleting {} patient data", patientDataToDelete.size());
      this.sharedService.audit(user, request, tenantService, AuditTypes.PatientDataDelete, String.join(", ", patientDataToDelete));
    }

    List<PatientList> patientLists = tenantService.getAllPatientLists();
    List<String> patientListsToDelete = patientLists.stream().filter(pl -> {
              try {
                return shouldDelete(tenantService.getConfig().getRetentionPeriod(), pl.getLastUpdated());
              } catch (DatatypeConfigurationException e) {
                return false;
              }
            })
            .map(PatientList::getId)
            .collect(Collectors.toList());

    if (patientListsToDelete.size() > 0) {
      tenantService.deletePatientLists(patientListsToDelete);

      logger.info("Deleting {} patient lists", patientListsToDelete.size());
      this.sharedService.audit(user, request, tenantService, AuditTypes.PatientListDelete, String.join(", ", patientListsToDelete));
    }

    return patientDataToDelete.size();
  }

  /**
   * @return
   */
  @GetMapping("/$test-fhir")
  public TestResponse test(@PathVariable String tenantId, @RequestParam String patientId, @RequestParam String measureId, @RequestParam String periodStart, @RequestParam String periodEnd) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    assert tenantService != null : "Tenant not instantiated";

    if (tenantService.getConfig().getFhirQuery() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant not configured to query FHIR");
    }

    TestResponse testResponse = new TestResponse();

    try {
      // Get the data
      logger.info("Testing querying/scooping data for the patient {}", patientId);

      PatientScoop patientScoop = this.applicationContext.getBean(PatientScoop.class);
      patientScoop.setShouldPersist(false);
      patientScoop.setTenantService(tenantService);
      patientScoop.setStopwatchManager(new StopwatchManager());

      PatientOfInterestModel poi = new PatientOfInterestModel();
      poi.setReference(patientId);

      ReportCriteria criteria = new ReportCriteria(measureId, periodStart, periodEnd);
      ReportContext context = new ReportContext();
      List<PatientOfInterestModel> pois = List.of(poi);

      patientScoop.loadInitialPatientData(criteria, context, pois);
      patientScoop.loadSupplementalPatientData(criteria, context, pois);
      String stats = patientScoop.getStopwatchManager().getStatistics();
      logger.info(stats);
    } catch (Exception ex) {
      testResponse.setSuccess(false);
      testResponse.setMessage(ex.getMessage());
    }

    return testResponse;
  }
}
