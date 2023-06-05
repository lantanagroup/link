package com.lantanagroup.link;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Stopwatch {
    private static final Logger logger = LoggerFactory.getLogger(Stopwatch.class);
    private final StopwatchManager manager;
    private final String name;
    private final Instant startTime;
    private Instant stopTime;

    public Stopwatch(StopwatchManager manager, String name) {
        this.manager = manager;
        this.name = name;
        this.startTime = Instant.now();
    }

    public synchronized void stop() {
        if (stopTime != null) {
            logger.warn("Stopwatch is already stopped");
            return;
        }
        stopTime = Instant.now();
        long millis = Duration.between(startTime, stopTime).toMillis();
        String duration = DurationFormatUtils.formatDurationHMS(millis);
        logger.debug("{} --- [{}]", duration, name);

        if (!this.manager.getCategories().containsKey(this.name)) {
            ConcurrentLinkedQueue<Long> list = new ConcurrentLinkedQueue<Long>();
            list.add(millis);
            this.manager.getCategories().put(this.name, list);
        } else {
            this.manager.getCategories().get(this.name).add(millis);
        }
    }
}
