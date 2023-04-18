package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.BulkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{tenantId}/bulk")
public class BulkController  extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(BulkController.class);

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

  @PostMapping("/$initiate-bulk-data")
  public void initiateBulkDataRequest(@PathVariable String tenantId) {
    //initiate a bulk request and save status url to persistence
  }

  @PostMapping("/$query-bulk-status")
  public void queryBulkStatus(@PathVariable String tenantId, @RequestBody String requestUrl) {
    //query bulk status and if output is ready, retrieve output and save to persistence
  }

  @GetMapping("/status/{id}")
  public BulkStatus getBulkStatusRecord(@PathVariable String id, @PathVariable String tenantId) {
    //retrieve bulk status record for an id
    return null;
  }

  @GetMapping("/status")
  public List<BulkStatus> getAllBulkStatuses(@PathVariable String tenantId) {
    //retrieve all pending bulk status requests for a tenantId
    return null;
  }

  @PostMapping("$execute-query-and-retrieval")
  public void executeQueryAndRetrieval(@PathVariable String tenantId){
    //for use in ApiInit to initiate all pending bulk status requests, can also be called via API
  }
}
