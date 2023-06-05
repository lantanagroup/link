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

        output.append("Report generation statistics:\n");
        output.append("Category\tTotal (s)\tTotal (ms)\tMean (s)\tMean (ms)\tMin (s)\tMin (ms)\tMax (s)\tMax (ms)\n");

        this.categories.keySet().forEach(c -> {

            double totalMillis = 0;
            Long minMillis = null;
            Long maxMillis = null;
            for (long duration : this.categories.get(c)) {
                totalMillis += duration;

                if (minMillis == null || minMillis > duration) {
                    minMillis = duration;
                }
                if (maxMillis == null || maxMillis < duration) {
                    maxMillis = duration;
                }
            }

            double totalSeconds = totalMillis / 1000;
            double meanMillis = totalMillis / this.categories.get(c).size();
            double meanSeconds = meanMillis / 1000;
            double minSeconds = minMillis / 1000;
            double maxSeconds = maxMillis / 1000;

            output.append(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                    c,
                    totalSeconds,
                    totalMillis,
                    meanSeconds,
                    meanMillis,
                    minSeconds,
                    minMillis,
                    maxSeconds,
                    maxMillis));
        });

        logger.info(output.toString());
    }

    public void reset() {
        this.categories = new ConcurrentHashMap<>();
    }
}
