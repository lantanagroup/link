package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.sender.FileSystemSenderConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
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

  private SecureRandom random = new SecureRandom();

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
      throw new IllegalArgumentException("Error: Not configured with a path in FileSystemSender to store the submission bundle");
    } else {
      path = Helper.expandEnvVars(this.config.getPath());
    }

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

  @SuppressWarnings("unused")
  @Override
  public void send(TenantService tenantService, Bundle submissionBundle, Report report, HttpServletRequest request, LinkCredentials user) throws Exception {
    FileSystemSenderConfig.Formats format = this.getFormat();
    String path = this.getFilePath().toString();
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

    if (this.config != null && this.config.getPretty()) {
      parser.setPrettyPrint(true);
    }

    logger.info("Encoding submission bundle to {}", format);

    if (StringUtils.isNotEmpty(this.config.getEncryptSecret())) {
      logger.debug("Encrypting the contents of the file-based submission");

      try (OutputStream os = new FileOutputStream(path)) {
        byte[] salt = new byte[8];
        this.random.nextBytes(salt);// Create key
        Cipher cipher = getCipher(this.config.getEncryptSecret(), salt);
        CipherOutputStream cipherOut = new CipherOutputStream(os, cipher);
        Writer writer = new OutputStreamWriter(cipherOut, StandardCharsets.UTF_8);
        os.write("Salted__".getBytes(StandardCharsets.US_ASCII));
        os.write(salt);
        parser.encodeResourceToWriter(submissionBundle, writer);
      }
    } else {
      try (Writer writer = new FileWriter(path, StandardCharsets.UTF_8)) {
        parser.encodeResourceToWriter(submissionBundle, writer);
      }
    }

    logger.info("Saved submission bundle to file system: {}", path);
  }
}
