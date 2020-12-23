package com.lantanagroup.nandina.send;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.QueryReport;
import com.lantanagroup.nandina.direct.Attachment;
import com.lantanagroup.nandina.direct.DirectSender;
import com.lantanagroup.nandina.query.QueryReportConverter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class PillboxSender implements IReportSender {
  @Override
  public void send(QueryReport report, NandinaConfig config, FhirContext ctx) throws Exception {
    String uniqueCsv = QueryReportConverter.getUnique(report);
    String medsCsv = QueryReportConverter.getMeds(report);
    String labsCsv = QueryReportConverter.getLabs(report);
    String dxCsv = QueryReportConverter.getDx(report);

    DirectSender sender = new DirectSender(config, ctx);
    InputStream uniqueCsvStream = new ByteArrayInputStream(uniqueCsv.getBytes());
    Attachment uniqueCsvAttachment = new Attachment(uniqueCsvStream, "text/csv", "unique.csv");
    InputStream medsCsvStream = new ByteArrayInputStream(medsCsv.getBytes());
    Attachment medsCsvAttachment = new Attachment(medsCsvStream, "text/csv", "meds.csv");
    InputStream labsCsvStream = new ByteArrayInputStream(labsCsv.getBytes());
    Attachment labsCsvAttachment = new Attachment(labsCsvStream, "text/csv", "labs.csv");
    InputStream dxCsvStream = new ByteArrayInputStream(dxCsv.getBytes());
    Attachment dxCsvAttachment = new Attachment(dxCsvStream, "text/csv", "dx.csv");

    sender.send("Nandina Pillbox Report",
            "Please see the attached files.",
            uniqueCsvAttachment,
            medsCsvAttachment,
            labsCsvAttachment,
            dxCsvAttachment);
  }
}
