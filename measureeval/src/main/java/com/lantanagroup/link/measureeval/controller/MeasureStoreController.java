package com.lantanagroup.link.measureeval.controller;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.measureeval.config.MeasureEvalConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RestController
public class MeasureStoreController {
  @Autowired
  private MeasureEvalConfig config;

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }

  @PostMapping("/$store-measure")
  public void storeMeasure(@RequestBody Bundle measureBundle) {
    FhirContext ctx = FhirContextProvider.getFhirContext();
    Path path = Paths.get(this.config.getMeasuresPath() + "/" + measureBundle.getId() + ".xml");
    String measureContentXML = ctx.newXmlParser().encodeResourceToString(measureBundle);
    try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
      writer.write(measureContentXML);
      writer.flush();
    }catch(IOException ex)
    {
      ex.printStackTrace();
    }
    System.out.println(this.config.getMeasuresPath());
  }
}
