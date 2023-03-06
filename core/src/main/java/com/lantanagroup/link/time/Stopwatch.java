package com.lantanagroup.link.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class Stopwatch implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(Stopwatch.class);

  protected final String task;
  private final Instant startTime;
  private Instant stopTime;

  public Stopwatch(String task) {
    this.task = task;
    this.startTime = Instant.now();
  }

  public static String format(Duration duration) {
    long minutes = duration.toMinutes();
    int seconds = duration.toSecondsPart();
    int millis = duration.toMillisPart();
    return String.format("%d:%02d.%03d", minutes, seconds, millis);
  }

  public void stop() {
    if (stopTime != null) {
      logger.warn("Stopwatch is already stopped");
      return;
    }
    stopTime = Instant.now();
    onStopped(Duration.between(startTime, stopTime));
  }

  protected void onStopped(Duration duration) {
    logger.debug("{} --- [{}]", format(duration), task);
  }

  @Override
  public final void close() {
    stop();
  }
}
