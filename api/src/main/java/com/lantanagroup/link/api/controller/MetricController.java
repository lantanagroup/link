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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metric")
public class MetricController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(MetricController.class);
  private static final String QUERY_CATEGORY = "query";
  private static final String EVALUATION_CATEGORY = "evaluate";
  private static final String VALIDATION_CATEGORY = "validation";
  private static final String REPORT_CATEGORY = "report";
  private static final String Submission_CATEGORY = "submission";
  private static final String EVENT_CATEGORY = "event";
  private static final String WEEKLY_PERIOD = "lastWeek";
  private static final String MONTHLY_PERIOD = "lastMonth";
  private static final String QUARTERLY_PERIOD = "lastQuarter";
  private static final String YEARLY_PERIOD = "lastYear";
  private static final List<String> validPeriods = List.of(WEEKLY_PERIOD, MONTHLY_PERIOD, QUARTERLY_PERIOD, YEARLY_PERIOD);


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
      logger.warn(String.format("An invalid report period of '%s' was submitted when requesting a metric report.", period));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period for metrics report.");
    }

    //build reporting period
    LocalDate endDate = LocalDate.now().plusDays(1);
    LocalDate startDate;
    switch(period) {
      case(WEEKLY_PERIOD):
        startDate = endDate.minusWeeks(11);
        break;
      case(MONTHLY_PERIOD):
        startDate = endDate.minusMonths(11);
        break;
      case(QUARTERLY_PERIOD):
        startDate = endDate.minusMonths(33); //3 months in a quarter x 11
        break;
      case(YEARLY_PERIOD):
        startDate = endDate.minusYears(11);
        break;
      default:
        logger.warn(String.format("An invalid report period of '%s' was submitted when requesting a metric report.", period));
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period for metrics report.");
    }

    //retrieve metrics
    List<Metrics> metrics = this.sharedService.getMetrics(startDate, endDate, tenantId, reportId);

    if(metrics.isEmpty()) {
      logger.info(String.format("No metrics where found for reporting period '%s'.", period));
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No reporting metrics could be found for the reporting period of '%s'.", period));
    }

    //calculate metrics based on period, we can assume that all categories have already been
    //filtered down based on tenant and/or report if those parameters were provided
    MetricsReportResponse report;
    switch(period) {
      case(WEEKLY_PERIOD):
        report = this.CalculateWeeklyMetrics(period, endDate, metrics);
        break;
      case(MONTHLY_PERIOD):
        report = this.CalculateMonthlyMetrics(period, endDate, metrics);
        break;
      case(QUARTERLY_PERIOD):
        report = this.CalculateQuarterlyMetrics(period, endDate, metrics);
        break;
      case(YEARLY_PERIOD):
        report = this.CalculateYearlyMetrics(period, endDate, metrics);
        break;
      default:
        logger.warn(String.format("An invalid report period of '%s' was submitted when requesting a metric report.", period));
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period for metrics report.");
    }

    return report;
  }

  //TODO: This functionality probably should be broken out into a service

  private MetricsReportResponse CalculateWeeklyMetrics(String period, LocalDate end, List<Metrics> metrics) {
    MetricsReportResponse report = new MetricsReportResponse();

    //-------------------------------------------------------------------
    // calculate query time metrics, category = query
    //-------------------------------------------------------------------
    //get current period metrics
    LocalDate start = end.minusWeeks(1);
    List<Metrics> periodMetrics = GetPeriodMetrics(start, end, metrics);

    //calculate average query time for the current period
    double currentQueryTimeAvg = CalculateQueryTimeAvg(periodMetrics);

    QueryTimeMetric queryTimeMetric = new QueryTimeMetric();
    queryTimeMetric.setAverage(currentQueryTimeAvg);
    report.setQueryTime(queryTimeMetric);

    //loop back through this historical data to produce previous 10 query time averages
    List<Double> historicalAvgs = new ArrayList<Double>();
    for (int i = 1; i <= 10; i++) {
      LocalDate historicalStart = start.minusWeeks(i);
      LocalDate historicalEnd = end.minusWeeks(i);
      List<Metrics> historicalMetrics = GetPeriodMetrics(historicalStart, historicalEnd, metrics);
      historicalAvgs.add(CalculateQueryTimeAvg(historicalMetrics));
    }

    //add historical averages to QueryTimeMetric
    double[] history = new double[historicalAvgs.size()];
    for (int i = 0; i < historicalAvgs.size(); i++) {
      history[i] = historicalAvgs.get(i);
    }
    queryTimeMetric.setHistory(history);

    //-------------------------------------------------------------------
    // calculate patients queried metrics
    //-------------------------------------------------------------------

    //-------------------------------------------------------------------
    // calculate patients reported metrics
    //-------------------------------------------------------------------

    //-------------------------------------------------------------------
    // calculate validation metrics
    //-------------------------------------------------------------------

    //-------------------------------------------------------------------
    // calculate evaluation metrics
    //-------------------------------------------------------------------

    return report;
  }

  private MetricsReportResponse CalculateMonthlyMetrics(String period, LocalDate end, List<Metrics> metrics) {
    MetricsReportResponse report = new MetricsReportResponse();
    return report;
  }

  private MetricsReportResponse CalculateQuarterlyMetrics(String period, LocalDate end, List<Metrics> metrics) {
    MetricsReportResponse report = new MetricsReportResponse();

    return report;
  }

  private MetricsReportResponse CalculateYearlyMetrics(String period, LocalDate end, List<Metrics> metrics) {
    MetricsReportResponse report = new MetricsReportResponse();

    return report;
  }

  private List<Metrics> GetPeriodMetrics(LocalDate start, LocalDate end, List<Metrics> metrics)  {

    return metrics.stream()
            .filter(obj -> obj.getTimestamp().after(java.sql.Date.valueOf(start)) && obj .getTimestamp().before(java.sql.Date.valueOf(end)))
            .collect(Collectors.toList());

  }
  private double CalculateQueryTimeAvg(List<Metrics> periodMetrics) {
    //get total query time spent in the metric period
    long totalQueryTimeSpent = 0;

    //if no period metrics exist, return 0
    if(!(periodMetrics.size() > 0)) {
      return totalQueryTimeSpent;
    }

    List<MetricData> periodQueryMetrics = GetPeriodMetricData(QUERY_CATEGORY, periodMetrics);
    for(MetricData data: periodQueryMetrics) {
      totalQueryTimeSpent += data.duration;
    }

    //determine query time averages
    List<String> reportIds = GetUniqueReportIds(periodMetrics);
    double currentQueryTimeAvg = (double) (totalQueryTimeSpent / (long) reportIds.size());

    return currentQueryTimeAvg;
  }

  private List<String> GetUniqueReportIds(List<Metrics> metrics) {

    //Build a unique list of report ids for processing
    return metrics.stream()
            .map(Metrics::getReportId)
            .distinct()
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
