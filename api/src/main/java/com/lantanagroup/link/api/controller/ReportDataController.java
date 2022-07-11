package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.IDataProcessor;
import lombok.Setter;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
public class ReportDataController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportDataController.class);

  @Autowired
  @Setter
  private ApplicationContext context;

  @PostMapping(value = "/data/{type}")
  public void retrieveData(@RequestBody() String csvContent, @PathVariable("type") String type) throws Exception {
    if(config.getDataProcessor() == null || config.getDataProcessor().get(type) == null || config.getDataProcessor().get(type).equals("")) {
      throw new HttpResponseException(400, "Bad Request, cannot find data processor.");
    }

    logger.debug("Receiving " + type + " data. Parsing...");

    Class<?> senderClass = Class.forName(this.config.getPatientIdResolver());
    IDataProcessor dataProcessor = (IDataProcessor) this.context.getBean(senderClass);

    dataProcessor.process(csvContent.getBytes(StandardCharsets.UTF_8), getFhirDataProvider());
  }
}
