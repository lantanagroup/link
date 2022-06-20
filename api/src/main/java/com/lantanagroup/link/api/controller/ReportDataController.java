package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.IDataProcessor;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.thsa.THSAConfig;
import com.lantanagroup.link.thsa.GenericCSVProcessor;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.Setter;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/fhir")
public class ReportDataController extends BaseController{
  private static final Logger logger = LoggerFactory.getLogger(ReportDataController.class);

  @Autowired
  @Setter
  private ApiConfig config;

  @Autowired
  private THSAConfig thsaConfig;

  @PostMapping(value = "/api/data/csv")
  public void retrieveCSVData(@RequestBody() String csvContent) throws Exception {

    if(config.getDataProcessor() == null || config.getDataProcessor().get("csv") == null || config.getDataProcessor().get("csv").equals("")) {
      throw new HttpResponseException(400, "Bad Request, cannot find data processor.");
    }

    logger.debug("Receiving CSV. Parsing...");
    GenericCSVProcessor genericCSVProcessor = new GenericCSVProcessor();
    genericCSVProcessor.process(csvContent.getBytes(), getFhirDataProvider(), thsaConfig);

    /*InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    CSVReader csvReader = new CSVReaderBuilder(bufferedReader).withSkipLines(1).build();
    List<String[]> csvData = csvReader.readAll();
    switch (type) {
      case "bed":
        // TODO
        break;
      case "ventilator":
        // TODO
        break;
    }*/
  }
}
