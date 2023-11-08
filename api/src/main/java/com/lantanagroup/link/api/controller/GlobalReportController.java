package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.ReportStatuses;
import com.lantanagroup.link.model.GlobalReportByStatusResponse;
import com.lantanagroup.link.model.GlobalReportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/report")
public class GlobalReportController extends BaseController {
  @Autowired
  private SharedService sharedService;

  @GetMapping
  public List<GlobalReportResponse> getAllReports() {
    //TODO - Daniel: Add paging and size

    return this.sharedService.getAllReports();
  }

  @GetMapping("status")
  public GlobalReportByStatusResponse getReportsByStatus(@RequestParam int count )  {
    GlobalReportByStatusResponse result = new GlobalReportByStatusResponse();

    List<GlobalReportResponse> reports = this.sharedService.getAllReports();

    result.setCompletedReports(reports.stream().filter(x -> x.getStatus() == ReportStatuses.Submitted).limit(count).collect(Collectors.toList()));
    //TODO: Add failed / in progress reports

    return result;
  }
}
