package com.lantanagroup.link.api.controller;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportDataController extends BaseController{
  private static final Logger logger = LoggerFactory.getLogger(ReportDataController.class);

  @PostMapping(value = "/data/csv?type=XXX")
  public void retrieveCSVData(@PathVariable("XXX") String type, @RequestBody() String csvContent) throws Exception {

    logger.debug("Receiving CSV. Parsing...");
    InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
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
    }
  }

}
