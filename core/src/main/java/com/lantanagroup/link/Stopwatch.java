package com.lantanagroup.link;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class Stopwatch {
  private static final Logger logger = LoggerFactory.getLogger(Stopwatch.class);

  private final String name;
  private final Instant startTime;
  private Instant stopTime;

  private Stopwatch(String name) {
    this.name = name;
    startTime = Instant.now();
  }

  public static Stopwatch start(String name) {
    return new Stopwatch(name);
  }

  public void stop() {
    if (stopTime != null) {
      logger.warn("Stopwatch is already stopped");
      return;
    }
    stopTime = Instant.now();
    long millis = Duration.between(startTime, stopTime).toMillis();
    String duration = DurationFormatUtils.formatDuration(millis, "s.SSS");
    logger.debug("{}: {}", name, duration);
  }
}
