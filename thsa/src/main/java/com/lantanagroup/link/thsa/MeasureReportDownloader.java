package com.lantanagroup.link.thsa;

import com.lantanagroup.link.Helper;
import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.*;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MeasureReportDownloader implements IReportDownloader {
    protected static final Logger logger = LoggerFactory.getLogger(MeasureReportDownloader.class);

    @Override
    public void download(String reportId, String downloadType, FhirDataProvider fhirDataProvider, HttpServletResponse response, FhirContext ctx, BundlerConfig config, EventService eventService) throws IOException, TransformerException {

        DocumentReference documentReference = fhirDataProvider.findDocRefForReport(reportId);

        List<MeasureReport> measureReports = new ArrayList<MeasureReport>();

        for (int i = 0; i < documentReference.getIdentifier().size(); i++) {
            String encodedReportId = null;

            try {
                encodedReportId = Helper.encodeForUrl(ReportIdHelper.getMasterMeasureReportId(reportId, documentReference.getIdentifier().get(i).getValue()));
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }

            MeasureReport measureReport = fhirDataProvider.getMeasureReportById(encodedReportId);
            measureReports.add(measureReport);
        }

        FhirBundler bundler = new FhirBundler(config, fhirDataProvider, eventService);
        Bundle bundle = bundler.generateBundle(measureReports, documentReference);

        logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");
        String responseBody = "";
        if (downloadType.equals("XML")) {
            responseBody = ctx.newXmlParser().encodeResourceToString(bundle);
            response.setContentType("application/xml");
        } else if (downloadType.equals("JSON")) {
            responseBody = ctx.newJsonParser().encodeResourceToString(bundle);
            response.setContentType("application/json");
        }

        if (Helper.validateHeaderValue(reportId)) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + reportId + ".xml\"");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report Id");
        }

        InputStream is = new ByteArrayInputStream(responseBody.getBytes());
        IOUtils.copy(is, response.getOutputStream());
        response.flushBuffer();

    }
}
