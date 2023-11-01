package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.Metrics;
import com.lantanagroup.link.model.MetricsReportResponse;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/metric")
public class MetricController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(MetricController.class);
  private static final List<String> validPeriods = List.of("lastWeek", "lastMonth", "lastQuarter", "lastYear");

  @Autowired
  private SharedService sharedService;

  /**
   * Returns calculated metrics based on the supplied reporting period and optional parameters.
   *
   * @param period - the reporting period for the metrics
   * @param reportId - filter metrics by a specific report
   * @param tenantId - filter metrics by the specific tenant
   * @return Returns a MetricsReportResponse
   * @throws ResponseStatusException Thrown when an invalid period has been supplied
   */
  @GetMapping("/{period}")
  public MetricsReportResponse getMetricReport(
          @PathVariable String period,
          @RequestParam(name = "tenantId", required = false) String tenantId,
          @RequestParam(name = "reportId", required = false) String reportId) {

    //check for valid report period type
    if (StringUtils.isEmpty(period) || !validPeriods.contains(period)) {
      logger.warn("An invalid report period was submitted when requesting a metric report.");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period for metrics report.");
    }

    //retrieve metrics
    LocalDate today = LocalDate.now();
    LocalDate lastWeek = today.minusDays(7);
    List<Metrics> metrics = this.sharedService.getMetrics(today, lastWeek, null, null);


    throw new NotImplementedException();
  }

}
