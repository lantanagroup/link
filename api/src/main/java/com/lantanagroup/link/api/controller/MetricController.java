package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.MetricData;
import com.lantanagroup.link.db.model.Metrics;
import com.lantanagroup.link.model.*;
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
  private static final List<String> validPeriods = List.of(Constants.WEEKLY_PERIOD, Constants.MONTHLY_PERIOD, Constants.QUARTERLY_PERIOD, Constants.YEARLY_PERIOD);

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
      case(Constants.WEEKLY_PERIOD):
        startDate = endDate.minusWeeks(11);
        break;
      case(Constants.MONTHLY_PERIOD):
        startDate = endDate.minusMonths(11);
        break;
      case(Constants.QUARTERLY_PERIOD):
        startDate = endDate.minusMonths(33); //3 months in a quarter x 11
        break;
      case(Constants.YEARLY_PERIOD):
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

      //generate 404 message for user
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("No reporting metrics could be found for the reporting period of '%s'", period));

      //add tenant id if provided
      if(!StringUtils.isEmpty(tenantId)) {
        sb.append(String.format(" and Tenant Id of '%s'", tenantId));
      }

      //add report id if provided
      if(!StringUtils.isEmpty(reportId)) {
        sb.append(String.format(" and Report Id of '%s'", reportId));
      }
      sb.append(".");

      throw new ResponseStatusException(HttpStatus.NOT_FOUND, sb.toString());
    }

    //calculate metrics based on period, we can assume that all categories have already been
    //filtered down based on tenant and/or report if those parameters were provided
    MetricsReportResponse report = this.calculatePeriodMetrics(period, endDate, metrics);

    return report;
  }

  //TODO: This functionality probably should be broken out into a service

  private MetricsReportResponse calculatePeriodMetrics(String period, LocalDate end, List<Metrics> metrics) {
    MetricsReportResponse report = new MetricsReportResponse();

    //get current period metrics
    LocalDate start;
    switch(period) {
      case(Constants.WEEKLY_PERIOD):
        start = end.minusWeeks(1);
        break;
      case(Constants.MONTHLY_PERIOD):
        start = end.minusMonths(1);
        break;
      case(Constants.QUARTERLY_PERIOD):
        start = end.minusMonths(3); //3 months in a quarter x 1
        break;
      case(Constants.YEARLY_PERIOD):
        start = end.minusYears(1);
        break;
      default:
        logger.warn(String.format("An invalid report period of '%s' was submitted when requesting a metric report.", period));
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period for metrics report.");
    }
    List<Metrics> periodMetrics = getPeriodMetrics(start, end, metrics);
    List<String> reportIds = getUniqueReportIds(periodMetrics);

    // calculate current query time metrics, category = query
    double currentQueryTimeAvg = calculateQueryTimeAvg(periodMetrics);
    QueryTimeMetric queryTimeMetric = new QueryTimeMetric();
    queryTimeMetric.setAverage(currentQueryTimeAvg);

    // calculate current patients queried metrics
    long currentTotalPatientsQueried = calculatePatientsQueried(periodMetrics);
    PatientsQueriedMetric patientsQueriedMetric = new PatientsQueriedMetric();
    patientsQueriedMetric.setTotal(currentTotalPatientsQueried);

    // calculate current patients reported metrics
    long currentTotalPatientsReported = calculatePatientsReported(periodMetrics);
    PatientsReportedMetric patientsReportedMetric = new PatientsReportedMetric();
    patientsReportedMetric.setTotal(currentTotalPatientsReported);

    // calculate current validation metrics
    double currentValidationTimeAvg = calculateValidationTimeAvg(periodMetrics);
    ValidationMetric validationTimeMetric = new ValidationMetric();
    validationTimeMetric.setAverage(currentValidationTimeAvg);

    // calculate current evaluation metrics
    double currentEvaluationTimeAvg = calculateEvaluationTimeAvg(periodMetrics);
    EvaluationMetric evaluationTimeMetric = new EvaluationMetric();
    evaluationTimeMetric.setAverage(currentEvaluationTimeAvg);

    //calculate current Validation Issues metrics
    double currentValidationIssueAvg = calculateValidationIssueAvg(periodMetrics);
    ValidationMetric validationIssueMetric = new ValidationMetric();
    validationIssueMetric.setAverage(currentValidationIssueAvg);

    // calculate current report generation metrics
    double currentReportTimeAvg = calculateReportGenerationTimeAvg(periodMetrics);
    EvaluationMetric reportGenMetric = new EvaluationMetric();
    reportGenMetric.setAverage(currentReportTimeAvg);

    //loop back through the historical data to produce previous 10 totals/averages
    double[] queryTimeHistory = new double[10];
    long[] patientsQueriedHistory = new long[10];
    long[] patientsReportedHistory = new long[10];
    double[] validationTimeHistory = new double[10];
    double[] evaluationTimeHistory = new double[10];
    double[] validationIssueHistory = new double[10];
    double[] reportGenerationHistory = new double[10];
    for (int i = 1; i <= 10; i++) {
      //get historical period
      LocalDate historicalStart;
      LocalDate historicalEnd;
      switch(period) {
        case(Constants.WEEKLY_PERIOD):
          historicalStart = start.minusWeeks(i);
          historicalEnd = end.minusWeeks(i);
          break;
        case(Constants.MONTHLY_PERIOD):
          historicalStart = start.minusMonths(i);
          historicalEnd = end.minusMonths(i);
          break;
        case(Constants.QUARTERLY_PERIOD):
          historicalStart = start.minusMonths(3*i); //3 months in a quarter x i
          historicalEnd = end.minusMonths(3*i);
          break;
        case(Constants.YEARLY_PERIOD):
          historicalStart = start.minusYears(i);
          historicalEnd = end.minusYears(i);
          break;
        default:
          logger.warn(String.format("An invalid report period of '%s' was submitted when requesting a metric report.", period));
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period for metrics report.");
      }
      List<Metrics> historicalMetrics = getPeriodMetrics(historicalStart, historicalEnd, metrics);

      //Calculate historical metrics
      queryTimeHistory[i-1] = calculateQueryTimeAvg(historicalMetrics);
      patientsQueriedHistory[i-1] = calculatePatientsQueried(historicalMetrics);
      patientsReportedHistory[i-1] = calculatePatientsReported(historicalMetrics);
      validationTimeHistory[i-1] = calculateValidationTimeAvg(historicalMetrics);
      evaluationTimeHistory[i-1] = calculateEvaluationTimeAvg(historicalMetrics);
      validationIssueHistory[i-1] = calculateValidationIssueAvg(historicalMetrics);
      reportGenerationHistory[i-1] = calculateReportGenerationTimeAvg(historicalMetrics);
    }

    //add historical averages for query time
    queryTimeMetric.setHistory(queryTimeHistory);
    report.setQueryTime(queryTimeMetric);

    //add historical totals for patients queried
    patientsQueriedMetric.setHistory(patientsQueriedHistory);
    report.setPatientsQueried(patientsQueriedMetric);

    //add historical totals for patients reported
    patientsReportedMetric.setHistory(patientsReportedHistory);
    report.setPatientsReported(patientsReportedMetric);

    //add historical averages for validation time
    validationTimeMetric.setHistory(validationTimeHistory);
    report.setValidation(validationTimeMetric);

    //add historical averages for evaluation time
    evaluationTimeMetric.setHistory(evaluationTimeHistory);
    report.setEvaluation(evaluationTimeMetric);

    //add historical averages for validation issues
    validationIssueMetric.setHistory(validationIssueHistory);
    report.setValidationIssues(validationIssueMetric);

    //add historical averages for validation issues
    reportGenMetric.setHistory(reportGenerationHistory);
    report.setReportGeneration(reportGenMetric);

    return report;
  }

  private List<Metrics> getPeriodMetrics(LocalDate start, LocalDate end, List<Metrics> metrics)  {

    return metrics.stream()
            .filter(obj -> obj.getTimestamp().after(java.sql.Date.valueOf(start)) && obj .getTimestamp().before(java.sql.Date.valueOf(end)))
            .collect(Collectors.toList());
  }

  private double calculateQueryTimeAvg(List<Metrics> periodMetrics) {
    //get total query time spent in the metric period
    double totalQueryTimeSpent = 0;

    //if no period metrics exist, return 0
    if(!(periodMetrics.size() > 0)) {
      return totalQueryTimeSpent;
    }

    List<MetricData> periodQueryMetrics = getPeriodMetricData(Constants.CATEGORY_QUERY, periodMetrics);
    for(MetricData data: periodQueryMetrics) {
      totalQueryTimeSpent += data.duration;
    }

    //determine query time averages
    //List<String> reportIds = GetUniqueReportIds(periodMetrics);
    double currentQueryTimeAvg = (totalQueryTimeSpent / Integer.max(1, periodQueryMetrics.size()));

    return currentQueryTimeAvg;
  }

  private long calculatePatientsQueried(List<Metrics> periodMetrics) {
    //get total patients queried in the metric period
    long totalPatientsQueried = 0;

    //if no period metrics exist, return 0
    if(!(periodMetrics.size() > 0)) {
      return totalPatientsQueried;
    }

    List<MetricData> periodQueryMetrics = getPeriodMetricData(Constants.CATEGORY_QUERY, "patient", periodMetrics);
    for(MetricData data: periodQueryMetrics) {
      totalPatientsQueried += data.count;
    }

    return totalPatientsQueried;
  }

  private long calculatePatientsReported(List<Metrics> periodMetrics) {
    //get total patients queried in the metric period
    long totalPatientsReported = 0;

    //if no period metrics exist, return 0
    if(!(periodMetrics.size() > 0)) {
      return totalPatientsReported;
    }

    List<MetricData> periodQueryMetrics = getPeriodMetricData(Constants.CATEGORY_REPORT, "store-measure-report", periodMetrics);
    for(MetricData data: periodQueryMetrics) {
      totalPatientsReported += data.count;
    }

    return totalPatientsReported;
  }

  private double calculateValidationTimeAvg(List<Metrics> periodMetrics) {
    //get total patients queried in the metric period
    double totalValidationTimeSpent = 0;

    //if no period metrics exist, return 0
    if(!(periodMetrics.size() > 0)) {
      return totalValidationTimeSpent;
    }

    List<MetricData> periodQueryMetrics = getPeriodMetricData(Constants.CATEGORY_VALIDATION, "validate", periodMetrics);
    for(MetricData data: periodQueryMetrics) {
      totalValidationTimeSpent += data.duration;
    }

    //determine query time averages
    //List<String> reportIds = GetUniqueReportIds(periodMetrics);
    double currentValidationTimeAvg = (totalValidationTimeSpent / Integer.max(1, periodQueryMetrics.size()));

    return currentValidationTimeAvg;
  }

  private double calculateEvaluationTimeAvg(List<Metrics> periodMetrics) {
    //get total patients queried in the metric period
    double totalEvaluationTimeSpent = 0;

    //if no period metrics exist, return 0
    if(!(periodMetrics.size() > 0)) {
      return totalEvaluationTimeSpent;
    }

    List<MetricData> periodQueryMetrics = getPeriodMetricData(Constants.CATEGORY_EVALUATE, periodMetrics);
    for(MetricData data: periodQueryMetrics) {
      totalEvaluationTimeSpent += data.duration;
    }

    //determine query time averages
    //List<String> reportIds = GetUniqueReportIds(periodMetrics);
    double currentEvaluationTimeAvg = (totalEvaluationTimeSpent / Integer.max(1, periodQueryMetrics.size()));

    return currentEvaluationTimeAvg;
  }

  private double calculateValidationIssueAvg(List<Metrics> periodMetrics) {
    //get total patients queried in the metric period
    double totalValidationIssues = 0;

    //if no period metrics exist, return 0
    if(!(periodMetrics.size() > 0)) {
      return totalValidationIssues;
    }

    List<MetricData> periodQueryMetrics = getPeriodMetricData(Constants.VALIDATION_ISSUE_CATEGORY, Constants.VALIDATION_ISSUE_TASK, periodMetrics);
    for(MetricData data: periodQueryMetrics) {
      totalValidationIssues += data.count;
    }

    double validationIssueAvg = (totalValidationIssues / Integer.max(1, periodQueryMetrics.size()));

    return validationIssueAvg;
  }

  private double calculateReportGenerationTimeAvg(List<Metrics> periodMetrics) {
    double totalReportGenTimeSpent = 0;

    if(!(periodMetrics.size() > 0)) {
      return totalReportGenTimeSpent;
    }

    List<MetricData> periodQueryMetrics = getPeriodMetricData(Constants.CATEGORY_REPORT, Constants.REPORT_GENERATION_TASK, periodMetrics);
    for(MetricData data: periodQueryMetrics) {
      totalReportGenTimeSpent += data.duration;
    }

    double currentReportGenTimeAvg = (totalReportGenTimeSpent / Integer.max(1, periodQueryMetrics.size()));

    return currentReportGenTimeAvg;
  }

  private List<String> getUniqueReportIds(List<Metrics> metrics) {

    //Build a unique list of report ids for processing
    return metrics.stream()
            .map(Metrics::getReportId)
            .distinct()
            .collect(Collectors.toList());
  }

  private List<MetricData> getPeriodMetricData(String category, List<Metrics> periodMetrics) {
    return periodMetrics.stream()
            .filter(obj -> obj.getCategory().equals(category))
            .map(Metrics::getData)
            .collect(Collectors.toList());
  }

  private List<MetricData> getPeriodMetricData(String category, String task, List<Metrics> periodMetrics) {
    return periodMetrics.stream()
            .filter(obj -> obj.getCategory().equals(category))
            .filter(obj -> obj.getTaskName().equals(task))
            .map(Metrics::getData)
            .collect(Collectors.toList());
  }

  @GetMapping("/size/{tenantId}")
  public PatientMeasureReportSizeSummary getPatientMeasureReportSizeSummary(
          @PathVariable String tenantId,
          @RequestParam(name = "patientId", required = false) String patientId,
          @RequestParam(name = "reportId", required = false) String reportId,
          @RequestParam(name = "measureId", required = false) String measureId) {

    var tenantService = TenantService.create(sharedService, tenantId);

    var reportSizes = tenantService.getPatientMeasureReportSize(patientId, reportId, measureId);

    var summary = new PatientMeasureReportSizeSummary();
    Double sum = 0.0;
    var measureCountMap = summary.getCountReportSizeByMeasureId();
    var reportCountMap = summary.getCountReportSizeByReportId();
    var patientCountMap = summary.getCountReportSizeByPatientId();

    var averageReportSizeByMeasureId = summary.getAverageReportSizeByMeasureId();
    var averageReportSizeByReportId = summary.getAverageReportSizeByReportId();
    var averageReportSizeByPatientId= summary.getAverageReportSizeByPatientId();

    for(var r : reportSizes)
    {
      var mId = r.getMeasureId();
      var pId = r.getPatientId();
      var rId = r.getReportId();

      sum += r.getSizeKb();

      if(measureCountMap.containsKey(mId))
        measureCountMap.put(mId, measureCountMap.get(mId) + 1);
      else {
        measureCountMap.put(mId, 1);
        averageReportSizeByMeasureId.put(mId, 0.0);
      }
      averageReportSizeByMeasureId.put(mId, averageReportSizeByMeasureId.get(mId) + r.getSizeKb());

      if(reportCountMap.containsKey(rId))
        reportCountMap.put(rId, reportCountMap.get(rId) + 1);
      else {
        reportCountMap.put(rId, 1);
        averageReportSizeByReportId.put(rId, 0.0);
      }

      averageReportSizeByReportId.put(rId, averageReportSizeByReportId.get(rId) + r.getSizeKb());

      if(patientCountMap.containsKey(pId))
        patientCountMap.put(pId, patientCountMap.get(pId) + 1);
      else {
        patientCountMap.put(pId, 1);
        averageReportSizeByPatientId.put(pId, 0.0);
      }
      averageReportSizeByPatientId.put(pId, averageReportSizeByPatientId.get(pId) + r.getSizeKb());
    }

    summary.setTotalSize(sum);
    summary.setReports(reportSizes);
    summary.setReportCount(reportSizes.size());
    summary.setAverageReportSize(sum/reportSizes.size());

    for(var kv : measureCountMap.entrySet()) {
      averageReportSizeByMeasureId.put(kv.getKey(), averageReportSizeByMeasureId.get(kv.getKey())/kv.getValue());
    }

    for(var kv : reportCountMap.entrySet()) {
      averageReportSizeByReportId.put(kv.getKey(), averageReportSizeByReportId.get(kv.getKey())/kv.getValue());
    }

    for(var kv : patientCountMap.entrySet()) {
      averageReportSizeByPatientId.put(kv.getKey(), averageReportSizeByPatientId.get(kv.getKey())/kv.getValue());
    }

    return summary;
  }
}
