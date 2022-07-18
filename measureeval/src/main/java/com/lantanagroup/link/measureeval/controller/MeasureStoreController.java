package com.lantanagroup.link.measureeval.controller;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.measureeval.config.MeasureEvalConfig;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RestController
public class MeasureStoreController {
  private static final Logger logger = LoggerFactory.getLogger(MeasureStoreController.class);
  @Autowired
  @Setter
  private MeasureEvalConfig config;

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }

  @PostMapping("/$store-measure")
  public void storeMeasure(@RequestBody Bundle measureBundle) throws IOException {
    if(StringUtils.isNotEmpty(measureBundle.getId())) {
      FhirContext ctx = FhirContextProvider.getFhirContext();
      String measureContentXML = ctx.newXmlParser().encodeResourceToString(measureBundle);
      Files.createDirectories(Paths.get(this.config.getMeasuresPath()));
      try (BufferedWriter writer = Files.newBufferedWriter(
              Paths.get(this.config.getMeasuresPath() + File.separator + measureBundle.getId() + ".xml"),
              StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
        writer.write(measureContentXML);
      }
      logger.debug(this.config.getMeasuresPath());
    }
  }
}
