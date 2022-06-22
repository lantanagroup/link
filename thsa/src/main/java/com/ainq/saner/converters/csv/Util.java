package com.ainq.saner.converters.csv;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Quantity;

public class Util {
    /**
     * Produces the inversion of a map. Assumes that map defines a function that is reversible.
     * @param <K>   The type of keys in the input map
     * @param <V>   The type of values in the input map
     * @param map   The map to invert.
     * @return  A map that performs the inverse transformation
     */
    public static <K, V> Map<V, K> invertMap(Map<K, V> map) {
        Map<V, K> inversion = new LinkedHashMap<>();
        for (Map.Entry<K, V> e: map.entrySet()) {
            K oldValue = inversion.put(e.getValue(), e.getKey());
            if (oldValue != null) {
                throw new IllegalArgumentException(e.getValue() + " maps to both " + oldValue + " and " + e.getKey());
            }
        }
        return inversion;
    }

    /**
     * Return true if the string expression in code matches a CodableConcept
     * @param code  The string to match
     * @param cc    The CodeableConcept to match against
     * @return      true if the string matches at least one coding of the CodeableConcept, false otherwise
     */
    public static boolean stringMatchesCodeableConcept(String code, CodeableConcept cc) {
        return cc.getCoding().stream().anyMatch(c -> stringMatchesCoding(code, c));
    }

    /**
     * Return true if the string expression in code matches a coding Coding
     * @param code      The string to match
     * @param coding    The Coding to match against
     * @return      true if the string matches, false otherwise
     */
    public static boolean stringMatchesCoding(String code, Coding coding) {
        if (code == null) {
            throw new NullPointerException();
        }
        String codingCode = coding.getCode();
        if (code.equals(codingCode)) {
            return true;
        }
        int i = code.indexOf('#');
        if (i < 0) {
            return false;
        }
        String system = coding.hasSystem() ? coding.getSystem() : "";
        return code.substring(0, i).equals(system) && code.substring(i + 1).equals(codingCode);
    }

    public static void setIntegerToString(IntegerType count, String value) {
        for (Extension ext: count.getExtensionsByUrl(Util.DATA_ABSENT_REASON_URL)) {
            count.getExtension().remove(ext);
        }
        count.setValue(null);

        if (value == null) {
            value = "";
        } else {
            value = value.trim();
        }
        if (value.matches("^[0-9]+$")) {
            count.setValueAsString(value);
        } else {
            count.addExtension(setDataAbsent(value));
        }
    }

    public static Extension setDataAbsent(String value) {
        Extension ex = new Extension();
        if (DATA_ABSENT_REASONS.contains(value.toLowerCase())) {
            ex.setUrl(DATA_ABSENT_REASON_URL).setValue(new CodeType(value));
        } else if (StringUtils.isBlank(value)) {
            ex.setUrl(DATA_ABSENT_REASON_URL).setValue(new CodeType("unknown"));
        } else {
            throw new IllegalArgumentException("Cannot set value to " + value);
        }
        return ex;
    }

    public static void setQuantityToString(Quantity measureScoreElement, String value) {
        measureScoreElement.setValue(null);
        measureScoreElement.setCode(null);
        measureScoreElement.setSystem(null);
        measureScoreElement.setUnit(null);
        for (Extension ext: measureScoreElement.getExtensionsByUrl(Util.DATA_ABSENT_REASON_URL)) {
            measureScoreElement.getExtension().remove(ext);
        }

        if (value == null) {
            value = "";
        } else {
            value = value.replace(" ", "");
        }
        if (value.endsWith("%")) {
            value = value.replace("%", "");
            measureScoreElement.setCode("%");
            measureScoreElement.setUnit("%");
            measureScoreElement.setSystem(UCUM_SYSTEM);
        }
        if (value.matches("^[+\\-]?[0-9]+(\\.[0-9]*)?([eE][+\\-][0-9]*)?$")) {
            measureScoreElement.setValue(new BigDecimal(value));
        } else {
            measureScoreElement.addExtension(setDataAbsent(value));
        }
    }

    private static final List<String> DATA_ABSENT_REASONS =
    Arrays.asList(
        "unknown", "asked-unknown", "temp-unknown", "not-asked", "asked-declined", "masked", "not-applicable",
        "unsupported", "as-text", "error", "non-a-number", "negative-infinity", "positive-infinity",
        "not-performed", "not-permitted");
    private static final String DATA_ABSENT_REASON_URL = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";
    private static final String UCUM_SYSTEM =  "http://unitsofmeasure.org";
}
