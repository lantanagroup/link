package com.lantanagroup.link.api.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/activity")
public class ActivityController {
  @GetMapping()
  public String getActivity() {
     return "";
  }
}
