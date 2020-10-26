package com.lantanagroup.nandina.send;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.PIHCQuestionnaireResponseGenerator;
import com.lantanagroup.nandina.QueryReport;
import com.lantanagroup.nandina.TransformHelper;
import com.lantanagroup.nandina.direct.DirectSender;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

public class PIHCSender implements IReportSender {
  @Override
  public void send(QueryReport report, JsonProperties config, FhirContext ctx) throws Exception {
    PIHCQuestionnaireResponseGenerator generator = new PIHCQuestionnaireResponseGenerator(report);
    QuestionnaireResponse questionnaireResponse = generator.generate();
    DirectSender sender = new DirectSender(config, ctx);

    if (config.getExportFormat().equalsIgnoreCase("json")) {
      sender.sendJSON("QuestionnaireResponse JSON", "Please see the attached questionnaireResponse json file", questionnaireResponse);
    } else if (config.getExportFormat().equalsIgnoreCase("xml")) {
      sender.sendXML("QuestionnaireResponse XML", "Please see the attached questionnaireResponse xml file", questionnaireResponse);
    } else if (config.getExportFormat().equalsIgnoreCase("csv")) {
      String csv = TransformHelper.questionnaireResponseToCSV(questionnaireResponse, ctx);
      sender.sendCSV("QuestionnaireResponse CSV", "Please see the attached questionnaireResponse csv file", csv);
    }
  }
}
