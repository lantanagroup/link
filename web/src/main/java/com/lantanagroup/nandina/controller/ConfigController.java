package com.lantanagroup.nandina.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController extends BaseController {
  @GetMapping("config/smart")
  public String getSmartLaunchClientId(@RequestParam() String issuer) {
    switch (issuer) {
      case "https://fhir-ehr-code.cerner.com/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d":
        return "df44deda-de6b-42a4-82a9-4af1ff944cf4";
    }

    return null;
  }
}
