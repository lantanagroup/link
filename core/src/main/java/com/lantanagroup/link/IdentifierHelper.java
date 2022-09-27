package com.lantanagroup.link;

import org.hl7.fhir.r4.model.Identifier;

public class IdentifierHelper {
  public static String toString(Identifier identifier) {
    if (!identifier.hasValue()) {
      throw new IllegalArgumentException("Identifier has no value");
    }
    return identifier.hasSystem()
            ? String.format("%s|%s", identifier.getSystem(), identifier.getValue())
            : identifier.getValue();
  }

  public static Identifier fromString(String string) {
    String[] components = string.split("\\|", 2);
    return components.length == 1
            ? new Identifier().setValue(components[0])
            : new Identifier().setSystem(components[0]).setValue(components[1]);
  }
}
