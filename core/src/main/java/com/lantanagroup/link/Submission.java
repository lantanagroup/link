package com.lantanagroup.link;

import ca.uhn.fhir.parser.IParser;
import lombok.Getter;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Submission {
  public static final String ORGANIZATION = "submitting-org.json";
  public static final String DEVICE = "submitting-device.json";
  public static final String QUERY_PLAN = "query-plan.yml";
  public static final String CENSUS = "census.json";
  public static final String AGGREGATE = "aggregate-%s.json";
  public static final String PATIENT = "patient-%s.json";
  public static final String SHARED = "shared-resources.json";
  public static final String VALIDATION = "validation-%s.json";
  public static final String PRE_QUAL = "validation.html";

  private final IParser parser;

  @Getter
  private final Path root;

  public Submission(boolean pretty) {
    parser = FhirContextProvider.getFhirContext().newJsonParser().setPrettyPrint(pretty);
    try {
      root = Files.createTempDirectory("link-");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void write(String filename, String content) throws IOException {
    write(filename, content.getBytes(StandardCharsets.UTF_8));
  }

  public void write(String filename, byte[] content) throws IOException {
    try (OutputStream stream = Files.newOutputStream(root.resolve(filename))) {
      stream.write(content);
    }
  }

  public void write(String filename, IBaseResource resource) throws IOException {
    try (Writer writer = Files.newBufferedWriter(root.resolve(filename))) {
      parser.encodeResourceToWriter(resource, writer);
    }
  }

  public <T extends IBaseResource> T read(Class<T> resourceType, String filename) throws IOException {
    try (Reader reader = Files.newBufferedReader(root.resolve(filename))) {
      return parser.parseResource(resourceType, reader);
    }
  }
}
