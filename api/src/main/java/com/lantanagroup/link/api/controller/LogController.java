package com.lantanagroup.link.api.controller;


import com.lantanagroup.link.Helper;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.model.LogMessage;
import com.lantanagroup.link.model.LogSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/log")
public class LogController extends BaseController {
  @Autowired
  private SharedService sharedService;

  /**
   * @param startDate The minimum/start date to return logs for
   * @param endDate   The maximum/end date to return logs for
   * @param severity  One or more severities to filter the results by
   * @param page      The page of logs to return
   * @param content   What content to look for in the message
   * @return
   */
  @GetMapping
  public LogSearchResponse getLogs(@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate, @RequestParam(required = false) String[] severity, @RequestParam(required = false, defaultValue = "1") int page, @RequestParam(required = false) String content) {
    Date startDateObj;
    Date endDateObj;

    startDateObj = Helper.parseFhirDate(startDate);
    endDateObj = Helper.parseFhirDate(endDate);

    List<LogMessage> logMessages = this.sharedService.findLogMessages(startDateObj, endDateObj, severity, page, content);
    LogSearchResponse res = new LogSearchResponse();
    res.setTotal(logMessages.size());
    res.setMessages(logMessages);
    return res;
  }
}
