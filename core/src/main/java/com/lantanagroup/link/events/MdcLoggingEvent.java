package com.lantanagroup.link.events;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MdcLoggingEvent implements ILoggingEvent {

  private ILoggingEvent mOrigEvent;

  public MdcLoggingEvent(ILoggingEvent origEvent) {
    mOrigEvent = origEvent;
  }

  @Override
  public String getThreadName() {
    return mOrigEvent.getThreadName();
  }

  @Override
  public Level getLevel() {
    return mOrigEvent.getLevel();
  }

  @Override
  public String getMessage() {
    return mOrigEvent.getMessage();
  }

  @Override
  public Object[] getArgumentArray() {
    var argList = mOrigEvent.getArgumentArray().clone();
    Map<String, String> mpm = mOrigEvent.getMDCPropertyMap();

    try {
      if (mpm.containsKey("reportId")) {
        var reportId = mpm.get("reportId");
        argList[0] = reportId;
        return argList;
      }
    } catch (Exception e) {
        return mOrigEvent.getArgumentArray();
    }
    return mOrigEvent.getArgumentArray();
  }

  @Override
  public String getFormattedMessage() {
    Map<String, String> mpm = mOrigEvent.getMDCPropertyMap();

    if (mpm.containsKey("reportId")) {
      var reportId = mpm.get("reportId");
      return String.format("[%s] ", reportId) + mOrigEvent.getFormattedMessage();
    }

    return mOrigEvent.getFormattedMessage();
  }

  @Override
  public String getLoggerName() {
    return mOrigEvent.getLoggerName();
  }

  @Override
  public LoggerContextVO getLoggerContextVO() {
    return mOrigEvent.getLoggerContextVO();
  }

  @Override
  public IThrowableProxy getThrowableProxy() {
    return mOrigEvent.getThrowableProxy();
  }

  @Override
  public StackTraceElement[] getCallerData() {
    return mOrigEvent.getCallerData();
  }

  @Override
  public boolean hasCallerData() {
    return mOrigEvent.hasCallerData();
  }

  @Override
  public Marker getMarker() {
    return mOrigEvent.getMarker();
  }

  @Override
  public List<Marker> getMarkerList() {
    return null;
  }

  @Override
  public Map<String, String> getMDCPropertyMap() {
    return mOrigEvent.getMDCPropertyMap();
  }

  @Override
  public Map<String, String> getMdc() {
    return mOrigEvent.getMdc();
  }

  @Override
  public long getTimeStamp() {
    return mOrigEvent.getTimeStamp();
  }

  @Override
  public int getNanoseconds() {
    return 0;
  }

  @Override
  public long getSequenceNumber() {
    return 0;
  }

  @Override
  public List<KeyValuePair> getKeyValuePairs() {
    return null;
  }

  @Override
  public void prepareForDeferredProcessing() {
    mOrigEvent.prepareForDeferredProcessing();
  }

}
