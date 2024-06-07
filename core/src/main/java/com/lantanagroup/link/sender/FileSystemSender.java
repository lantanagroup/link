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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

  public Path getFilePath(String type) {
    String suffix = "";

    if (this.config.getIsBundle()) {
      switch (this.getFormat()) {
        case XML:
          suffix = ".xml";
          break;
        case JSON:
          suffix = ".json";
          break;
        default:
          throw new RuntimeException("No suffix specified for submission file");
      }
    }

    return this.getFilePath(type, suffix);
  }

  public Path getFilePath(String type, String suffix) {
    String path;

    if (this.config == null || this.config.getPath() == null || this.config.getPath().isEmpty()) {
      logger.info("Not configured with a path to store the submission bundle. Using the system temporary directory");
      throw new IllegalArgumentException("Error: Not configured with a path in FileSystemSender to store the submission bundle");
    } else {
      path = Helper.expandEnvVars(this.config.getPath());
    }

    String fileName = type + "-" + (new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date())) + suffix;

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

  private String getValidationReportHTML(TenantService tenantService, Report report) throws IOException {
    return new ValidationCategorizer()
            .getValidationCategoriesAndResultsHtml(tenantService, report);
  }

  @SuppressWarnings("unused")
  @Override
  public void send(EventService eventService, TenantService tenantService, Report report, HttpServletRequest request, LinkCredentials user) throws Exception {
    if (this.config == null) {
      logger.info("Not configured to send to file system");
      return;
    }

    logger.info(
            "Encoding submission bundle to {} {} encryption",
            this.config.getFormat(),
            StringUtils.isEmpty(this.config.getEncryptSecret()) ? "without" : "with");


    OperationOutcome outcome = tenantService.getValidationResultsOperationOutcome(
            report.getId(),
            OperationOutcome.IssueSeverity.INFORMATION,
            null);

    FhirBundler bundler = new FhirBundler(eventService, tenantService);

    if (this.config.getIsBundle()) {
      Bundle submissionBundle = bundler.generateBundle(report);
      this.saveToFile(submissionBundle, this.getFilePath("submission").toString());
      this.saveToFile(outcome, this.getFilePath("validation").toString());

      // Save validation results as HTML
      logger.debug("Saving validation results as HTML");
      String html = this.getValidationReportHTML(tenantService, report);
      if (StringUtils.isNotEmpty(html)) {
        this.saveToFile(html.getBytes(StandardCharsets.UTF_8), this.getFilePath("validation", ".html").toString());
      }
    } else {
      Submission submission = bundler.generateSubmission(report, this.config.getPretty());
      String orgId = tenantService.getOrganizationID();
      String path = orgId != null && !orgId.isEmpty() ?
              this.getFilePath(orgId).toString() :
              this.getFilePath("submission").toString();
      Files.move(submission.getRoot(), Paths.get(path));
    }
  }
}
