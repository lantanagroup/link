package com.lantanagroup.nandina.download;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.QueryReport;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.QueryReportConverter;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PillboxDownloader implements IReportDownloader {

  @Override
  public void download(QueryReport report, HttpServletResponse response, FhirContext ctx, NandinaConfig config) throws IOException, TransformerException {
    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(bs);
    zos.putNextEntry(new ZipEntry("unique.csv"));
    zos.write(QueryReportConverter.getUnique(report).getBytes());
    zos.closeEntry();
    zos.putNextEntry(new ZipEntry("meds.csv"));
    zos.write(QueryReportConverter.getMeds(report).getBytes());
    zos.closeEntry();
    zos.putNextEntry(new ZipEntry("dx.csv"));
    zos.write(QueryReportConverter.getDx(report).getBytes());
    zos.closeEntry();
    zos.putNextEntry(new ZipEntry("lab.csv"));
    zos.write(QueryReportConverter.getLabs(report).getBytes());
    zos.closeEntry();
    zos.close();

    response.setContentType("application/zip");
    response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    response.setHeader("Content-Disposition", "attachment; filename=\"report.zip\"");
    InputStream is = new ByteArrayInputStream(bs.toByteArray());
    IOUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }
}
