package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class TerminologyHelper {
  protected static final Logger logger = LoggerFactory.getLogger(TerminologyHelper.class);
  private FhirContext ctx = FhirContext.forR4();
  private IParser xmlParser = ctx.newXmlParser();
  private Map<String, Set<String>> codeSets = new HashMap<String, Set<String>>();


  public static String extractCodes(FhirContext fhirContext, File valueSetFile) throws Exception {
    String valueSetFileContents = Files.readString(valueSetFile.toPath());
    ValueSet valueSet;
    List<String> codes = new ArrayList();

    if (valueSetFile.getName().toLowerCase().endsWith(".xml")) {
      valueSet = (ValueSet) fhirContext.newXmlParser().parseResource(valueSetFileContents);
    } else if (valueSetFile.getName().toLowerCase().endsWith(".json")) {
      valueSet = (ValueSet) fhirContext.newJsonParser().parseResource(valueSetFileContents);
    } else {
      throw new Exception("Value set file " + valueSetFile.getName() + " has an unexpected extension.");
    }

    if (valueSet.getExpansion() != null) {
      for (ValueSet.ValueSetExpansionContainsComponent contains : valueSet.getExpansion().getContains()) {
        if (!Helper.isNullOrEmpty(contains.getCode())) {
          codes.add(contains.getCode());
        }
      }
    }

    if (codes.size() == 0) {
      throw new Exception("Value set " + valueSetFile.getName() + " does not have an expansion with codes");
    }

    return String.join(",", codes);
  }


  private Set<String> valueSetToSet(ValueSet vs) {
    Set<String> set = new HashSet<String>();
    if (vs.hasExpansion() && vs.getExpansion().hasContains()) {
      for (ValueSetExpansionContainsComponent contains : vs.getExpansion().getContains()) {
        Set<String> c = extractContainsSet(contains);
        set.addAll(c);
      }
    }
    return set;
  }

  private Set<String> extractContainsSet(ValueSetExpansionContainsComponent contains) {
    Set<String> set = new HashSet<String>();
    if (contains.hasSystem() && contains.hasCode()) {
      set.add(contains.getSystem() + "|" + contains.getCode());
    }
    if (contains.hasContains()) {
      for (ValueSetExpansionContainsComponent childContains : contains.getContains()) {
        Set<String> c = extractContainsSet(childContains);
        set.addAll(c);
      }
    }
    return set;
  }

  public Set<String> getValueSetAsSetString(String oid) {
    Set<String> codeSet = null;
    if (codeSets.containsKey(oid)) {
      codeSet = codeSets.get(oid);
    } else {
      ValueSet vs = getValueSet(oid);
      codeSet = valueSetToSet(vs);
      codeSets.put(oid, codeSet);
    }
    return codeSet;
  }

  private ValueSet getValueSet(String oid) {
    String uri = "classpath:" + oid + ".xml";
    ValueSet vs = loadValueSet(uri);
    return vs;
  }


  private ValueSet loadValueSet(String valueSetUri) {
    ValueSet vs = null;
    logger.info("Loading ValueSet " + valueSetUri);

    try {
      if (valueSetUri.startsWith("https://") || valueSetUri.startsWith("http://")) {
        // TODO: pull the value set from the web
        // or look up cannonical http/https value set URIs from a FHIR server.
        // Just because ValueSet.uri starts with http does not mean it is actually accessible at that URL
        throw new Exception("Nandina does not yet support http/https value sets.");
      } else {
        File valueSetFile = ResourceUtils.getFile(valueSetUri);
        String vsString = new String(Files.readAllBytes(valueSetFile.toPath()));
        vs = (ValueSet) xmlParser.parseResource(vsString);
      }
    } catch (Exception ex) {
      logger.error("Could not load " + valueSetUri, ex);
    }

    logger.info(String.format("Loaded ValueSet %s", valueSetUri));
    return vs;
  }
}
