package com.lantanagroup.link.api.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/dashboard")
public class DashboardController extends BaseController {
  //TODO - Daniel: Do we want tenant specific/filtered dashboards?
  @GetMapping()
  public String getDashboard() {
    return "";
  }
}
