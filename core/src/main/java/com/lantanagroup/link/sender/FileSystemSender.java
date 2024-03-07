package com.lantanagroup.link.sender;

import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.link.*;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.sender.FileSystemSenderConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.primitives.Bytes.concat;

@Component
public class FileSystemSender extends GenericSender implements IReportSender {
  protected static Logger logger = LoggerFactory.getLogger(FileSystemSender.class);

  @Autowired
  @Setter
  private FileSystemSenderConfig config;

  private final SecureRandom random = new SecureRandom();

  private FileSystemSenderConfig.Formats getFormat() {
    if (this.config == null || this.config.getFormat() == null) {
      return FileSystemSenderConfig.Formats.JSON;
    }
    return this.config.getFormat();
  }

  public Path getFilePath() {
    String suffix = "";
    String path;

    if (this.config == null || this.config.getPath() == null || this.config.getPath().isEmpty()) {
      logger.info("Not configured with a path to store the submission bundle. Using the system temporary directory");
      throw new IllegalArgumentException("Error: Not configured with a path in FileSystemSender to store the submission bundle");
    } else {
      path = Helper.expandEnvVars(this.config.getPath());
    }

    if (this.config.getIsBundle()) {
      switch (this.getFormat()) {
        case XML:
          suffix = ".xml";
          break;
        case JSON:
          suffix = ".json";
          break;
      }
    }

    String fileName = "submission-" + (new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date())) + suffix;

    return Paths.get(path, fileName);
  }

  public static Cipher getCipher(String password, byte[] salt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchProviderException {
    byte[] passAndSalt = concat(password.getBytes(StandardCharsets.UTF_8), salt);
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] key = md.digest(passAndSalt);
    SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
    md.reset();
    byte[] iv = Arrays.copyOfRange(md.digest(concat(key, passAndSalt)), 0, 16);
    //Adding BouncyCastle provider ("BC") to allow for GCM
    Security.addProvider(new BouncyCastleProvider());
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
    return cipher;
  }

  private void saveToFile(byte[] content, String path) throws Exception {
    if (StringUtils.isNotEmpty(this.config.getEncryptSecret())) {
      try (OutputStream os = new FileOutputStream(path)) {
        byte[] salt = new byte[8];
        this.random.nextBytes(salt);// Create key
        Cipher cipher = getCipher(this.config.getEncryptSecret(), salt);
        CipherOutputStream cipherOut = new CipherOutputStream(os, cipher);
        os.write("Salted__".getBytes(StandardCharsets.US_ASCII));
        os.write(salt);
        cipherOut.write(content);
      }
    } else {
      try (OutputStream os = new FileOutputStream(path)) {
        os.write(content);
      }
    }
  }

  private void saveToFile(Resource resource, String path) throws Exception {
    if (resource == null) {
      return;
    }

    FileSystemSenderConfig.Formats format = this.getFormat();
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

    if (this.config.getPretty()) {
      parser.setPrettyPrint(true);
    }

    if (StringUtils.isNotEmpty(this.config.getEncryptSecret())) {
      try (OutputStream os = new FileOutputStream(path)) {
        byte[] salt = new byte[8];
        this.random.nextBytes(salt);// Create key
        Cipher cipher = getCipher(this.config.getEncryptSecret(), salt);
        CipherOutputStream cipherOut = new CipherOutputStream(os, cipher);
        Writer writer = new OutputStreamWriter(cipherOut, StandardCharsets.UTF_8);
        os.write("Salted__".getBytes(StandardCharsets.US_ASCII));
        os.write(salt);
        parser.encodeResourceToWriter(resource, writer);
      }
    } else {
      try (Writer writer = new FileWriter(path, StandardCharsets.UTF_8)) {
        parser.encodeResourceToWriter(resource, writer);
      }
    }

    logger.info("Saved submission bundle to file system: {}", path);
  }

  private void saveToFolder(Bundle bundle, String path, TenantService tenantService, String reportId) throws Exception {
    File folder = new File(path);

    if (!folder.exists() && !folder.mkdirs()) {
      logger.error("Unable to create folder {}", path);
      throw new RuntimeException("Unable to create folder for submission");
    }

    FhirBundleProcessor fhirBundleProcessor = new FhirBundleProcessor(bundle);

    // Save link resources
    logger.debug("Saving link resources");
    if (fhirBundleProcessor.getLinkOrganization() != null) {
      this.saveToFile(fhirBundleProcessor.getLinkOrganization().getResource(), Paths.get(path, "organization.json").toString());
    }

    if (fhirBundleProcessor.getLinkDevice() != null) {
      this.saveToFile(fhirBundleProcessor.getLinkDevice().getResource(), Paths.get(path, "device.json").toString());
    }

    if (fhirBundleProcessor.getLinkQueryPlanLibrary() != null) {
      Library library = (Library) fhirBundleProcessor.getLinkQueryPlanLibrary().getResource();
      this.saveToFile(library.getContentFirstRep().getData(), Paths.get(path, "query-plan.yml").toString());
    }

    // Save aggregate measure reports
    logger.debug("Saving aggregate measure reports");
    List<Bundle.BundleEntryComponent> aggregates = fhirBundleProcessor.getAggregateMeasureReports();
    if (aggregates != null && !aggregates.isEmpty()) {
      for (Bundle.BundleEntryComponent aggregate : aggregates) {
        Resource aggregateReport = aggregate.getResource();
        this.saveToFile(aggregateReport, Paths.get(path, String.format("aggregate-%s.json", aggregateReport.getIdElement().getIdPart())).toString());
      }
    }

    // Save census lists
    logger.debug("Saving census lists");
    List<Bundle.BundleEntryComponent> lists = fhirBundleProcessor.getLinkCensusLists();
    if (lists != null && !lists.isEmpty()) {
      for (Bundle.BundleEntryComponent entry : lists) {
        Resource list = entry.getResource();
        this.saveToFile(list,
                Paths.get(path, String.format("census-%s.json", list.getIdElement().getIdPart())).toString());
      }
    }

    // Save patient resources
    logger.debug("Saving patient resources as patient bundles");
    Set<String> patientIds = fhirBundleProcessor.getPatientResources().keySet();
    if (!patientIds.isEmpty()) {
      for (String patientId : patientIds) {
        Bundle patientBundle = new Bundle();
        patientBundle.setType(Bundle.BundleType.COLLECTION);
        patientBundle.getEntry().addAll(fhirBundleProcessor.getPatientResources().get(patientId));

        this.saveToFile(patientBundle, Paths.get(path, String.format("patient-%s.json", patientId)).toString());
      }
    }

    // Save other resources
    logger.debug("Saving other resources");
    Bundle otherResourcesBundle = new Bundle();
    otherResourcesBundle.setType(Bundle.BundleType.COLLECTION);
    otherResourcesBundle.getEntry().addAll(fhirBundleProcessor.getOtherResources());

    if (otherResourcesBundle.hasEntry()) {
      this.saveToFile(otherResourcesBundle, Paths.get(path, "other-resources.json").toString());
    }

    // Annotating validation results with what file each issue occurs in and saving
    logger.debug("Annotating and saving validation results");
    OperationOutcome outcome =
            tenantService.getValidationResultsOperationOutcome(reportId, OperationOutcome.IssueSeverity.INFORMATION, null);
    List<OperationOutcome.OperationOutcomeIssueComponent> issues = outcome.getIssue();
    issues.forEach(i -> {
      String expression = i.getExpression().get(0).toString();
      String resourceType = expression.substring(expression.indexOf("ofType(") + 7, expression.indexOf(")"));
      String id = expression.substring(expression.indexOf("id = '") + 6, expression.indexOf("')"));
      if(resourceType.equals("Organization") && id.equals(fhirBundleProcessor.getLinkOrganization().getResource().getIdElement().getIdPart())) {
        i.setDiagnostics("organization.json");
      }
      else if(resourceType.equals("Device") && id.equals(fhirBundleProcessor.getLinkDevice().getResource().getIdElement().getIdPart())) {
        i.setDiagnostics("device.json");
      }
      else if(resourceType.equals("MeasureReport") && aggregates != null &&
              aggregates.stream().anyMatch(a -> a.getResource().getIdElement().getIdPart().equals(id))) {
        i.setDiagnostics(String.format("aggregate-%s.json", id));
      }
      else if(resourceType.equals("List") && lists != null &&
              lists.stream().anyMatch(l -> l.getResource().getIdElement().getIdPart().equals(id))) {
        i.setDiagnostics(String.format("census-%s.json", id));
      }
      else if(resourceType.equals("Patient") && patientIds.stream().anyMatch(p -> p.equals(id))) {
        i.setDiagnostics(String.format("patient-%s.json", id));
      }
      else if(resourceType.equals("Library") &&
              id.equals(fhirBundleProcessor.getLinkQueryPlanLibrary().getResource().getIdElement().getIdPart())) {
        i.setDiagnostics("query-plan.json");
      }
      else {
        i.setDiagnostics("other-resources.json");
      }
    });
    this.saveToFile(outcome, Paths.get(path, "validation-results.json").toString());

  }

  @SuppressWarnings("unused")
  @Override
  public void send(TenantService tenantService, Bundle submissionBundle, Report report, HttpServletRequest request, LinkCredentials user) throws Exception {
    if (this.config == null) {
      logger.info("Not configured to send to file system");
      return;
    }

    logger.info(
            "Encoding submission bundle to {} {} encryption",
            this.config.getFormat(),
            StringUtils.isEmpty(this.config.getEncryptSecret()) ? "without" : "with");

    String path = this.getFilePath().toString();

    if (this.config.getIsBundle()) {
      this.saveToFile(submissionBundle, path);
    } else {
      this.saveToFolder(submissionBundle, path, tenantService, report.getId());
    }
  }
}
