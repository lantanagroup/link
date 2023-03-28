package com.lantanagroup.link.time;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StopwatchManager {
  private static final int COUNT_WIDTH = 6;
  private static final int DURATION_WIDTH = 11;

  private final Map<String, List<Duration>> durationsByTask = new LinkedHashMap<>();

  public Stopwatch start(String task) {
    return new ManagedStopwatch(task);
  }

  private synchronized void record(String task, Duration duration) {
    durationsByTask.computeIfAbsent(task, _task -> new ArrayList<>()).add(duration);
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
      String task = entry.getKey();
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
    public ManagedStopwatch(String task) {
      super(task);
    }

    @Override
    protected void onStopped(Duration duration) {
      super.onStopped(duration);
      record(task, duration);
    }
  }
}
