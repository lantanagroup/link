package com.lantanagroup.link.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.lantanagroup.link.model.ApiInfoModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URL;

@RestController
@RequestMapping("/api")
public class ApiController {
  @GetMapping
  public ApiInfoModel getVersionInfo() {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      URL buildFile = this.getClass().getClassLoader().getResource("build.yml");

      if (buildFile == null) return new ApiInfoModel("dev");

      ApiInfoModel apiInfo = mapper.readValue(buildFile, ApiInfoModel.class);
      return apiInfo;
    } catch (IOException ex) {
      return new ApiInfoModel("dev");
    }
  }
}
