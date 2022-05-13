package com.lantanagroup.link.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.lantanagroup.link.model.ApiInfoModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

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

  @GetMapping(value = "/docs", produces = "text/yaml")
  public String getDocs() throws IOException {
    ClassPathResource resource = new ClassPathResource("swagger.yml");
    InputStream inputStream = resource.getInputStream();
    String content = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
    return content;
  }
}
