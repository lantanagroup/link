package com.lantanagroup.link.api.controller;


import com.lantanagroup.link.model.LogMessage;
import com.lantanagroup.link.model.LogSearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/api/log")
public class LogController extends BaseController {
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
    // TODO: Replace with actual implementation
    LogSearchResponse res = new LogSearchResponse();
    res.setTotal(2);
    res.getMessages().add(new LogMessage(new Date(), "ERROR", "This is a test message #1", "com.lantanagroup.link.api.ApiInit"));
    res.getMessages().add(new LogMessage(new Date(), "INFO", "This is a test message #2", "com.lantanagroup.link.api.controller.ApiController"));
    return res;
  }
}
