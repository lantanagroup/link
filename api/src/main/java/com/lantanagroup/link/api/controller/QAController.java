package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.model.ReportBase;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/qa/{tenantId}")
@ConditionalOnProperty("api.allowQaEndpoints")
public class QAController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(QAController.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private SharedService sharedService;

  @Autowired
  private ApiConfig config;

  @Setter
  private ExecutorService executorService;

  public QAController() {

    this.executorService = Executors.newFixedThreadPool(10);
  }

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }


  @DeleteMapping("/$deletePatientListById/{id}")
  public void deletePatientListById(@PathVariable String tenantId, @PathVariable UUID id){
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    List<Report> reports = tenantService.getReportsByPatientListId(id);
    if (!reports.isEmpty()) {
      String message = String.format(
              "The following reports reference the specified patient list: %s",
              reports.stream().map(ReportBase::getId).collect(Collectors.joining(", ")));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    tenantService.deletePatientListById(id);
  }

  @DeleteMapping("/$deleteAllPatientData")
  public void deleteAllPatientData(@PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    tenantService.deleteAllPatientData();
  }

  @DeleteMapping("/$deletePatientFromList/{patientListId}/{patientId}")
  public void deletePatientByIDAndListId(@PathVariable String tenantId, @PathVariable UUID patientListId, @PathVariable String patientId){
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    tenantService.deletePatientByListAndPatientId(patientId,patientListId);
  }

  @DeleteMapping("/$deleteReportById/{reportId}")
  public void deleteReport(@PathVariable String tenantId,@PathVariable String reportId){
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    tenantService.deleteReport(reportId);
  }
}
