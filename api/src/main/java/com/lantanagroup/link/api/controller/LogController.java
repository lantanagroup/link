package com.lantanagroup.link.api.controller;


import com.lantanagroup.link.model.LogMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/log")
public class LogController extends BaseController {
  @GetMapping
  public List<LogMessage> getLogs(String startDate, String endDate, String[] severity) {
    // TODO: Replace with actual implementation
    return List.of(
            new LogMessage(new Date(), "ERROR", "This is a test message #1", "com.lantanagroup.link.api.ApiInit"),
            new LogMessage(new Date(), "INFO", "This is a test message #2", "com.lantanagroup.link.api.controller.ApiController"));
  }
}
