package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.AuditTypes;
import com.lantanagroup.link.model.TestResponse;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
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
  public TestResponse test(@PathVariable String tenantId, @RequestParam(required = false) String patientId, @RequestParam(required = false) String patientIdentifier) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    assert tenantService != null : "Tenant not instantiated";

    if (tenantService.getConfig().getFhirQuery() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant not configured to query FHIR");
    }

    if (StringUtils.isEmpty(patientId) && StringUtils.isEmpty(patientIdentifier)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either patientId or patientIdentifier are required");
    }

    TestResponse testResponse = new TestResponse();

    try {
      IGenericClient client = FhirContextProvider.getFhirContext()
              .newRestfulGenericClient(tenantService.getConfig().getFhirQuery().getFhirServerBase());
      client.registerInterceptor(new LoggingInterceptor());
      client.registerInterceptor(new HapiFhirAuthenticationInterceptor(tenantService, this.applicationContext));

      if (StringUtils.isEmpty(patientId)) {
        Bundle bundle = client.search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().code(patientIdentifier))
                .returnBundle(Bundle.class)
                .execute();
        List<String> patientIds = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Patient)
                .map(Resource::getIdPart)
                .collect(Collectors.toList());
        if (patientIds.isEmpty()) {
          testResponse.setMessage("Patients not found");
        } else {
          testResponse.setMessage("Patients found: " + String.join(", ", patientIds));
        }
      } else {
        try {
          client.read()
                  .resource(Patient.class)
                  .withId(StringUtils.removeStart(patientId, "Patient/"))
                  .execute();
          testResponse.setMessage("Patient found");
        } catch (ResourceNotFoundException | ResourceGoneException e) {
          testResponse.setMessage("Patient not found or gone");
        }
      }
    } catch (Exception ex) {
      testResponse.setSuccess(false);
      testResponse.setMessage(ex.getMessage());
    }

    return testResponse;
  }
}
