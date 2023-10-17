package com.lantanagroup.link.time;

import com.lantanagroup.link.db.model.MetricData;
import com.lantanagroup.link.db.model.Metrics;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

import com.lantanagroup.link.db.SharedService;

@Component
public class StopwatchManager {
  private static final int COUNT_WIDTH = 6;
  private static final int DURATION_WIDTH = 11;

  private SharedService sharedService;

  private final Map<String, List<Duration>> durationsByTask = new LinkedHashMap<>();

  public StopwatchManager(SharedService sharedService){
    this.sharedService = sharedService;
  }

  public Stopwatch start(String task, String category) {
    return new ManagedStopwatch(task, category);
  }

  private synchronized void record(String task, String category, Duration duration) {
    durationsByTask.computeIfAbsent(task + ":" + category, _task -> new ArrayList<>()).add(duration);
  }

  public synchronized String getStatistics() {
    int taskWidth = durationsByTask.keySet().stream()
            .mapToInt(String::length)
            .max()
            .orElse(DURATION_WIDTH);
    Formatter formatter = new Formatter(taskWidth, COUNT_WIDTH, DURATION_WIDTH);
    StringBuilder statistics = new StringBuilder();
    statistics.append(formatter.getHeaderLine("Task", "Count", "Total", "Min", "Mean", "Max"));
    for (Map.Entry<String, List<Duration>> entry : durationsByTask.entrySet()) {
      String key = entry.getKey();
      String task = key.substring(0, key.indexOf(":"));
      List<Duration> durations = entry.getValue();
      statistics.append(formatter.getValueLine(
            task,
            durations.size(),
            getTotal(durations),
            getMin(durations),
            getMean(durations),
            getMax(durations)));
    }
    return statistics.toString();
  }

  public synchronized void storeMetrics(String tenantId, String reportId) {
    List<Metrics> metrics = new ArrayList<>();
    for (Map.Entry<String, List<Duration>> entry : durationsByTask.entrySet()) {
      List<Duration> durations = entry.getValue();
      String key = entry.getKey();
      String task = key.substring(0, key.indexOf(":"));
      String category = key.substring(key.indexOf(":") + 1);
      Metrics metric = new Metrics();
      metric.setTenantId(tenantId);
      metric.setReportId(reportId);
      metric.setTaskName(task);
      metric.setCategory(category);
      metric.setTimestamp(new Date());

      MetricData data = new MetricData();
      data.count = durations.size();
      data.duration = getTotal(durations).toMillis();
      metric.setData(data);

      metrics.add(metric);
    }
    this.sharedService.saveMetrics(metrics);
  }

  private Duration getTotal(List<Duration> durations) {
    return durations.stream().reduce(Duration.ZERO, Duration::plus);
  }

  private Duration getMin(List<Duration> durations) {
    return durations.stream().min(Duration::compareTo).orElseThrow();
  }

  private Duration getMean(List<Duration> durations) {
    return getTotal(durations).dividedBy(durations.size());
  }

  private Duration getMax(List<Duration> durations) {
    return durations.stream().max(Duration::compareTo).orElseThrow();
  }

  public synchronized void reset() {
    durationsByTask.clear();
  }

  private static class Formatter {
    private static final String SEPARATOR = "  ";

    private final int taskWidth;
    private final int countWidth;
    private final int durationWidth;

    public Formatter(int taskWidth, int countWidth, int durationWidth) {
      this.taskWidth = taskWidth;
      this.countWidth = countWidth;
      this.durationWidth = durationWidth;
    }

    public String getHeaderLine(String task, String count, String... durations) {
      List<String> headers = new ArrayList<>();
      headers.add(StringUtils.center(task, taskWidth));
      headers.add(StringUtils.center(count, countWidth));
      for (String duration : durations) {
        headers.add(StringUtils.center(duration, durationWidth));
      }
      return String.join(SEPARATOR, headers) + System.lineSeparator();
    }

    public String getValueLine(String task, int count, Duration... durations) {
      List<String> values = new ArrayList<>();
      values.add(StringUtils.rightPad(task, taskWidth));
      values.add(StringUtils.leftPad(Integer.toString(count), countWidth));
      for (Duration duration : durations) {
        values.add(StringUtils.leftPad(Stopwatch.format(duration), durationWidth));
      }
      return String.join(SEPARATOR, values) + System.lineSeparator();
    }
  }

  private class ManagedStopwatch extends Stopwatch {
    public ManagedStopwatch(String task, String category) {
      super(task, category);
    }

    @Override
    protected void onStopped(Duration duration) {
      super.onStopped(duration);
      record(task, category, duration);
    }
  }
}
