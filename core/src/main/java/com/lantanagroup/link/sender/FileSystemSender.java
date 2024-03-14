package com.lantanagroup.link.sender;

import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.link.*;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.sender.FileSystemSenderConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.validation.ValidationCategorizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

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

  private void saveToFolder(Bundle bundle, String path, TenantService tenantService, Report report) throws Exception {
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
    if (!fhirBundleProcessor.getAggregateMeasureReports().isEmpty()) {
      for (int i = 0; i < fhirBundleProcessor.getAggregateMeasureReports().size(); i++) {
        this.saveToFile(fhirBundleProcessor.getAggregateMeasureReports().get(i).getResource(), Paths.get(path, String.format("aggregate-%d.json", i + 1)).toString());
      }
    }

    // Save census lists
    logger.debug("Saving census lists");
    if (fhirBundleProcessor.getLinkCensusLists() != null && !fhirBundleProcessor.getLinkCensusLists().isEmpty()) {
      for (int i = 0; i < fhirBundleProcessor.getLinkCensusLists().size(); i++) {
        this.saveToFile(fhirBundleProcessor.getLinkCensusLists().get(i).getResource(), Paths.get(path, String.format("census-%d.json", i + 1)).toString());
      }
    }

    // Save patient resources
    logger.debug("Saving patient resources as patient bundles");

    if (!fhirBundleProcessor.getPatientResources().isEmpty()) {
      for (String patientId : fhirBundleProcessor.getPatientResources().keySet()) {
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

    // Save validation results as HTML
    logger.debug("Saving validation results as HTML");
    String html = new ValidationCategorizer().getValidationCategoriesAndResultsHtml(tenantService, report);
    if (StringUtils.isNotEmpty(html)) {
      this.saveToFile(html.getBytes(StandardCharsets.UTF_8), Paths.get(path, "validation-report.html").toString());
    }
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
      this.saveToFolder(submissionBundle, path, tenantService, report);
    }
  }
}
