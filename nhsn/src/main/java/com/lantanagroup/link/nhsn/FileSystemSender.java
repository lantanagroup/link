package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import com.lantanagroup.link.config.sender.FileSystemSenderConfig;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class FileSystemSender extends GenericSender implements IReportSender {
    protected static Logger logger = LoggerFactory.getLogger(FileSystemSender.class);

    @Autowired
    @Setter
    private FileSystemSenderConfig config;

    public static String expandEnvVars(String text) {
        Map<String, String> envMap = System.getenv();
        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            text = text.replaceAll("%" + key + "%", value);
        }
        return text;
    }

    private FileSystemSenderConfig.Formats getFormat() {
        if (this.config == null || this.config.getFormat() == null) {
            return FileSystemSenderConfig.Formats.JSON;
        }
        return this.config.getFormat();
    }

    public Path getFilePath() {
        String suffix = ".json";

        switch (this.getFormat()) {
            case XML:
                suffix = ".xml";
                break;
        }

        String fileName = "submission-" + (new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date())) + suffix;
        String path;

        if (this.config == null || this.config.getPath() == null || this.config.getPath().length() == 0) {
            logger.info("Not configured with a path to store the submission bundle. Using the system temporary directory");
            path = System.getProperty("java.io.tmpdir");
        } else {
            path = expandEnvVars(this.config.getPath());
        }

        return Paths.get(path, fileName);
    }

    @Override
    public void send(List<MeasureReport> masterMeasureReports, DocumentReference documentReference, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, BundlerConfig bundlerConfig) throws Exception {
        Bundle bundle = this.generateBundle(documentReference, masterMeasureReports, fhirDataProvider, bundlerConfig);

        FileSystemSenderConfig.Formats format = this.getFormat();
        String content;
        IParser parser;

        switch (format) {
            case JSON:
                parser = FhirContextProvider.getFhirContext().newJsonParser();
                break;
            case XML:
                parser = FhirContextProvider.getFhirContext().newXmlParser();
                break;
            default:
                throw new Exception(String.format("Unexpected format %s", format));
        }

        if (this.config != null && this.config.getPretty() == true) {
            parser.setPrettyPrint(true);
        }

        logger.info(String.format("Encoding submission bundle to %s", format));
        content = parser.encodeResourceToString(bundle);
        logger.info(String.format("Done encoding submission bundle to %s", format));

        Path filePath = this.getFilePath();
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));

        logger.info(String.format("Done saving submission bundle/report to %s", filePath.toString()));
    }

    @Override
    public String bundle(Bundle bundle, FhirDataProvider fhirProvider, String type) {
        // TODO: Not sure why this override is required by GenericSender if its not always used
        return null;
    }
}
