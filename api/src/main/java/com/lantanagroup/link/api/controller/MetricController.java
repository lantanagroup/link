package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.MetricData;
import com.lantanagroup.link.db.model.Metrics;
import com.lantanagroup.link.model.MetricsReportResponse;
import com.lantanagroup.link.model.QueryTimeMetric;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metric")
public class MetricController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(MetricController.class);
  private static final List<String> validPeriods = List.of("lastWeek", "lastMonth", "lastQuarter", "lastYear");
  private static final String QUERY_CATEGORY = "query";
  private static final String EVALUATION_CATEGORY = "evaluate";
  private static final String VALIDATION_CATEGORY = "validation";
  private static final String REPORT_CATEGORY = "report";
  private static final String EVENT_CATEGORY = "event";

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

    //build reporting period
    LocalDate endDate = LocalDate.now();
    LocalDate startDate;
    switch(period) {
      case("lastWeek"):
        startDate = endDate.minusWeeks(11);
        break;
      case("lastMonth"):
        startDate = endDate.minusMonths(11);
        break;
      case("lastQuarter"):
        startDate = endDate.minusMonths(33); //3 months in a quarter x 11
        break;
      case("lastYear"):
        startDate = endDate.minusYears(11);
        break;
      default:
        logger.warn("An invalid report period was submitted when requesting a metric report.");
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period for metrics report.");
    }

    //retrieve metrics
    List<Metrics> metrics = this.sharedService.getMetrics(startDate, endDate, null, null);

    //Build a unique list of report ids for processing
    List<String> uniqueReportIds = StringUtils.isEmpty(reportId) ?  metrics.stream()
            .map(Metrics::getReportId)
            .distinct()
            .collect(Collectors.toList()) : List.of(reportId);

    //calculate metrics based on period, we can assume that all categories have already been
    //filtered down based on tenant and/or report if those were provided
    MetricsReportResponse report;
    switch(period) {
      case("lastWeek"):
        report = this.CalculateWeeklyMetrics(period, endDate, uniqueReportIds, metrics);
        break;
      case("lastMonth"):
        report = this.CalculateMonthlyMetrics(period, endDate, uniqueReportIds, metrics);
        break;
      case("lastQuarter"):
        report = this.CalculateQuarterlyMetrics(period, endDate, uniqueReportIds, metrics);
        break;
      case("lastYear"):
        report = this.CalculateYearlyMetrics(period, endDate, uniqueReportIds, metrics);
        break;
      default:
        logger.warn("An invalid report period was submitted when requesting a metric report.");
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period for metrics report.");
    }

    return report;
  }

  //TODO: This functionality probably should be broken out into a service potentially

  private MetricsReportResponse CalculateWeeklyMetrics(String period, LocalDate end, List<String> reportIds, List<Metrics> metrics) {
    MetricsReportResponse report = new MetricsReportResponse();

    //-------------------------------------------------------------------
    // calculate query time metrics, category = query
    //-------------------------------------------------------------------
    //get current period metrics
    LocalDate start = end.minusWeeks(1);
    List<Metrics> periodMetrics = GetPeriodMetrics(start, end, metrics);

    //get total query time spent in the metric period
    long totalQueryTimeSpent = 0;
    List<MetricData> periodQueryMetrics = GetPeriodMetricData(QUERY_CATEGORY, periodMetrics);
    for(MetricData data: periodQueryMetrics) {
      totalQueryTimeSpent += data.duration;
    }

    Double currentQueryTimeAvg = (double) (totalQueryTimeSpent / (long) reportIds.size());


    //calculate patients queried metrics

    //calculate patients reported metrics

    //calculate validation metrics

    //calculate evaluation metrics

    return report;
  }

  private MetricsReportResponse CalculateMonthlyMetrics(String period, LocalDate end, List<String> reportIds, List<Metrics> metrics) {
    MetricsReportResponse report = new MetricsReportResponse();

    return report;
  }

  private MetricsReportResponse CalculateQuarterlyMetrics(String period, LocalDate end, List<String> reportIds, List<Metrics> metrics) {
    MetricsReportResponse report = new MetricsReportResponse();

    return report;
  }

  private MetricsReportResponse CalculateYearlyMetrics(String period, LocalDate end, List<String> reportIds, List<Metrics> metrics) {
    MetricsReportResponse report = new MetricsReportResponse();

    return report;
  }

  private List<Metrics> GetPeriodMetrics(LocalDate start, LocalDate end, List<Metrics> metrics)  {

    return metrics.stream()
            .filter(obj -> obj.getTimestamp().after(java.sql.Date.valueOf(start)) && obj .getTimestamp().before(java.sql.Date.valueOf(end)))
            .collect(Collectors.toList());

  }

  private List<MetricData> GetPeriodMetricData(String category, List<Metrics> periodMetrics) {
    return periodMetrics.stream()
            .filter(obj -> obj.getCategory().equals(category))
            .map(Metrics::getData)
            .collect(Collectors.toList());
  }
  private List<MetricData> GetReportMetricData(String reportId, String category, List<Metrics> periodMetrics) {
    return periodMetrics.stream()
            .filter(obj -> obj.getReportId().equals(reportId))
            .filter(obj -> obj.getCategory().equals(category))
            .map(Metrics::getData)
            .collect(Collectors.toList());
  }

}
