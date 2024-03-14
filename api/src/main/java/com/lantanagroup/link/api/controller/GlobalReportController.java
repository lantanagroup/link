package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Helper;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.ReportStatuses;
import com.lantanagroup.link.model.GlobalReportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/report")
public class GlobalReportController extends BaseController {
  @Autowired
  private SharedService sharedService;

  @GetMapping
  public List<GlobalReportResponse> getReports(
          @RequestParam(required = false, defaultValue = "") String tenantId,
          @RequestParam(required = false, defaultValue = "") String status,
          @RequestParam(required = false, defaultValue = "") String startDate,
          @RequestParam(required = false, defaultValue = "") String endDate,
          @RequestParam(required = false, defaultValue = "") String measureIds,
          @RequestParam(required = false, defaultValue = "1") int page,
          @RequestParam(required = false, defaultValue = "10") int count) throws ParseException {

    List<GlobalReportResponse> reports = this.sharedService.getAllReports();

    if (!startDate.isEmpty()) {
      Date date = Helper.parseFhirDate(startDate);

      reports = reports.stream().filter(x -> {
        try {
          return Helper.parseFhirDate(x.getPeriodStart()).after(date) || Helper.parseFhirDate(x.getPeriodStart()).equals(date);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList());
    }

    if (!endDate.isEmpty()) {
      Date date = Helper.parseFhirDate(endDate);

      reports = reports.stream().filter(x -> {
        try {
          return Helper.parseFhirDate(x.getPeriodEnd()).before(date) || Helper.parseFhirDate(x.getPeriodStart()).equals(date);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList());
    }

    if (!tenantId.isEmpty()) {
      reports = reports.stream().filter(x -> x.getTenantId().equals(tenantId)).collect(Collectors.toList());
    }

    if (!status.isEmpty()) {
      reports = reports.stream().filter(x -> x.getStatus().equals(ReportStatuses.valueOf(status))).collect(Collectors.toList());
    }

    if (!measureIds.isEmpty()) {
      String[] ids = measureIds.split(",");
      reports = reports.stream().filter(x -> {
        HashSet<String> measureIdsSet = new HashSet<>(x.getMeasureIds());
        for(String measureId : ids) {
          if(measureIdsSet.contains(measureId)) {
            return true;
          }
        }
        return false;
      }).collect(Collectors.toList());
    }

    reports = reports
            .stream()
            .skip((long) (page - 1) * count)
            .limit(count)
            .collect(Collectors.toList());

    return reports;
  }
}
