package com.lantanagroup.link;

import ca.uhn.fhir.parser.IParser;
import lombok.Getter;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Submission {
  public static final String ORGANIZATION = "submitting-org.json";
  public static final String DEVICE = "submitting-device.json";
  public static final String QUERY_PLAN = "query-plan.yml";
  public static final String CENSUS = "census.json";
  public static final String AGGREGATE = "aggregate-%s.json";
  public static final String PATIENT = "patient-%s.json";
  public static final String VALIDATION = "validation-%s.json";

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

  public void write(String filename, byte[] bytes) throws IOException {
    try (OutputStream stream = Files.newOutputStream(root.resolve(filename))) {
      stream.write(bytes);
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
