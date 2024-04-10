package com.lantanagroup.link.appender;

import ch.qos.logback.classic.db.DBAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.lantanagroup.link.events.MdcLoggingEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class MdcDbAppender extends DBAppender {

  @Override
  protected void subAppend(ILoggingEvent event, Connection connection, PreparedStatement insertStatement)
          throws Throwable {
    // use a special event with custom formatted message
    MdcLoggingEvent customEvent = new MdcLoggingEvent(event);
    super.subAppend(customEvent, connection, insertStatement);
  }

}
