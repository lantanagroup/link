package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.AuditTypes;
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
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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

  /**
   * Deletes all census lists, patient data bundles, and measure reports stored on the server if their retention period
   * has been reached.
   */
  @DeleteMapping(value = "/$expunge")
  public Integer expungeData(@PathVariable String tenantId, @AuthenticationPrincipal LinkCredentials user, HttpServletRequest request) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    assert tenantService != null : "Tenant not instantiated";

    if (StringUtils.isEmpty(tenantService.getConfig().getRetentionPeriod())) {
      logger.error("Tenant retention period is not configured");
      return 0;
    }

    Period retentionPeriod = Period.parse(tenantService.getConfig().getRetentionPeriod());
    Date date = Date.from(LocalDate.now().minus(retentionPeriod)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant());
    int patientDataCount = tenantService.deletePatientDataRetrievedBefore(date);
    if (patientDataCount > 0) {
      logger.info("Deleted {} patient data", patientDataCount);
      this.sharedService.audit(user, request, tenantService, AuditTypes.PatientDataDelete, null);
    }

    // TODO: Delete from dbo.aggregate, dbo.patientMeasureReport, dbo.reportPatientList, and dbo.report

    int patientListCount = tenantService.deletePatientListsLastUpdatedBefore(date);
    if (patientListCount > 0) {
      logger.info("Deleted {} patient lists", patientListCount);
      this.sharedService.audit(user, request, tenantService, AuditTypes.PatientListDelete, null);
    }

    return patientDataCount;
  }

  /**
   * @return
   */
  @GetMapping("/$test-fhir")
  public TestResponse test(@PathVariable String tenantId, @RequestParam String patientId, @RequestParam String patientIdentifier, @RequestParam String measureId, @RequestParam String periodStart, @RequestParam String periodEnd) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    assert tenantService != null : "Tenant not instantiated";

    if (tenantService.getConfig().getFhirQuery() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant not configured to query FHIR");
    }

    if (StringUtils.isEmpty(patientId) && StringUtils.isEmpty(patientIdentifier)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either patientId or patientIdentifier are required");
    }

    if (StringUtils.isEmpty(measureId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "measureId is required");
    }

    if (StringUtils.isEmpty(periodStart)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "periodStart is required");
    }

    if (StringUtils.isEmpty(periodEnd)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "periodEnd is required");
    }

    TestResponse testResponse = new TestResponse();

    try {
      // Get the data
      logger.info("Testing querying/scooping data for the patient {}", patientId);

      PatientScoop patientScoop = this.applicationContext.getBean(PatientScoop.class);
      patientScoop.setShouldPersist(false);
      patientScoop.setTenantService(tenantService);
      patientScoop.setStopwatchManager(new StopwatchManager(this.sharedService));

      PatientOfInterestModel poi = new PatientOfInterestModel();
      poi.setReference(patientId);
      poi.setIdentifier(patientIdentifier);

      ReportCriteria criteria = new ReportCriteria(measureId, periodStart, periodEnd);
      ReportContext context = new ReportContext();
      List<PatientOfInterestModel> pois = List.of(poi);

      patientScoop.loadInitialPatientData(criteria, context, pois);
      patientScoop.loadSupplementalPatientData(criteria, context, pois);
      //No report to get an id from, set the reportId to the tenant to indicate this metric applies to the tenant as a whole
      patientScoop.getStopwatchManager().storeMetrics(tenantId, tenantId, Constants.TEST_QUERY);
    } catch (Exception ex) {
      testResponse.setSuccess(false);
      testResponse.setMessage(ex.getMessage());
    }

    return testResponse;
  }
}
