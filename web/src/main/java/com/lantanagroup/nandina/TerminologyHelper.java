package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.ValueSet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TerminologyHelper {
    private static String getFileContents(File file) throws IOException {
        StringBuilder contents = new StringBuilder();
        InputStream is = new FileInputStream(file);
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));
        String line = buf.readLine();

        while (line != null) {
            contents.append(line).append("\n");
            line = buf.readLine();
        }

        return contents.toString();
    }

    public static String extractCodes(FhirContext fhirContext, File valueSetFile) throws Exception {
        String valueSetFileContents = getFileContents(valueSetFile);
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
}
