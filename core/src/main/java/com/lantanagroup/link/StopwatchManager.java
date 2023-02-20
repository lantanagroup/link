package com.lantanagroup.link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class StopwatchManager {
  private static final Logger logger = LoggerFactory.getLogger(StopwatchManager.class);

  private ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> categories = new ConcurrentHashMap<>();

  public synchronized Stopwatch start(String name) {
    return new Stopwatch(this, name);
  }

  public ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> getCategories() {
    return this.categories;
  }

  public void print() {
    StringBuilder output = new StringBuilder();
    output.append("\n");

    this.categories.keySet().forEach(c -> {
      output.append(String.format("Category \"%s\":\n", c));
      output.append(String.format("\tCount: %s\n", this.categories.get(c).size()));

      double totalMilliseconds = 0;
      for (long duration : this.categories.get(c)) {
        totalMilliseconds += duration;
      }
      double totalSeconds = totalMilliseconds / 1000;

      output.append(String.format("\tTotal Time: %s (secs) %s (millis)\n", totalSeconds, totalMilliseconds));
    });

    logger.info(output.toString());
  }

  public void reset() {
    this.categories = new ConcurrentHashMap<>();
  }
}
