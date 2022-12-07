package com.ainq.fhir.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.IParserErrorHandler;
import ca.uhn.fhir.rest.api.EncodingEnum;

public class YamlUtils {

    public static String fromYaml(String yaml) throws JsonMappingException, JsonProcessingException {
        ObjectMapper yamlReader = newYAMLMapper();
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }

    public static String fromYaml(InputStream in) throws IOException {
        ObjectMapper yamlReader = newYAMLMapper();
        Object obj = yamlReader.readValue(in, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }

    public static String fromYaml(Reader r) throws IOException {
        ObjectMapper yamlReader = newYAMLMapper();
        Object obj = yamlReader.readValue(r, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }

    public static String toYaml(String jsonString) throws IOException {
        // parse JSON
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        // save it as YAML
        String jsonAsYaml = newYAMLMapper().writeValueAsString(jsonNodeTree);
        return jsonAsYaml;
    }

    public static void toYaml(String jsonString, Writer w) throws IOException {
        // parse JSON
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        newYAMLMapper().writeValue(w, jsonNodeTree);
    }

    public static void toYaml(String jsonString, OutputStream os) throws IOException {
        // parse JSON
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        // save it as YAML
        newYAMLMapper().writeValue(os, jsonNodeTree);
    }


    public static YAMLMapper newYAMLMapper() {
        YAMLMapper m = new YAMLMapper();
        return m.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
        .disable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .disable(YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS)
        .disable(YAMLGenerator.Feature.SPLIT_LINES);
    }

    public static YamlParser newYamlParser(FhirContext context) {
        return new YamlParser(context);
    }

    public static class YamlParser implements IParser {
        private final IParser jsonParser;
        YamlParser(FhirContext context) {
            jsonParser = context.newJsonParser();
        }

        @Override
        public String encodeResourceToString(IBaseResource theResource) throws DataFormatException {

            try {
                return toYaml(jsonParser.encodeResourceToString(theResource));
            } catch (IOException e) {
                throw new DataFormatException("Error Converting to YAML", e);
            }
        }

        @Override
        public void encodeResourceToWriter(IBaseResource theResource, Writer theWriter)
            throws IOException, DataFormatException {

            try {
                theWriter.write(toYaml(jsonParser.encodeResourceToString(theResource)));
            } catch (IOException e) {
                throw new DataFormatException("Error Converting to YAML", e);
            }
        }

        @Override
        public IIdType getEncodeForceResourceId() {
            return jsonParser.getEncodeForceResourceId();
        }

        @Override
        public EncodingEnum getEncoding() {
            return jsonParser.getEncoding();
        }

        @Override
        public List<Class<? extends IBaseResource>> getPreferTypes() {
            return jsonParser.getPreferTypes();
        }

        @Override
        public boolean isOmitResourceId() {
            return jsonParser.isOmitResourceId();
        }

        @Override
        public Boolean getStripVersionsFromReferences() {
            return jsonParser.getStripVersionsFromReferences();
        }

        @Override
        public boolean isSummaryMode() {
            return jsonParser.isSummaryMode();
        }

        @Override
        public <T extends IBaseResource> T parseResource(Class<T> theResourceType, Reader theReader)
            throws DataFormatException {
            try {
                return jsonParser.parseResource(theResourceType, fromYaml(theReader));
            } catch (IOException e) {
                throw new DataFormatException("Error Converting from YAML", e);
            }
        }

        @Override
        public <T extends IBaseResource> T parseResource(Class<T> theResourceType, InputStream theInputStream)
            throws DataFormatException {
            try {
                return jsonParser.parseResource(theResourceType, fromYaml(theInputStream));
            } catch (IOException e) {
                throw new DataFormatException("Error Converting from YAML", e);
            }
        }

        @Override
        public <T extends IBaseResource> T parseResource(Class<T> theResourceType, String theString)
            throws DataFormatException {
            try {
                return jsonParser.parseResource(theResourceType, fromYaml(theString));
            } catch (Exception e) {
                throw new DataFormatException("Error Converting from YAML", e);
            }
        }

        @Override
        public IBaseResource parseResource(Reader theReader) throws ConfigurationException, DataFormatException {
            try {
                return jsonParser.parseResource(fromYaml(theReader));
            } catch (Exception e) {
                throw new DataFormatException("Error Converting from YAML", e);
            }
        }

        @Override
        public IBaseResource parseResource(InputStream theInputStream)
            throws ConfigurationException, DataFormatException {
            try {
                return jsonParser.parseResource(fromYaml(theInputStream));
            } catch (Exception e) {
                throw new DataFormatException("Error Converting from YAML", e);
            }
        }

        @Override
        public IBaseResource parseResource(String theMessageString) throws ConfigurationException, DataFormatException {
            try {
                return jsonParser.parseResource(fromYaml(theMessageString));
            } catch (Exception e) {
                throw new DataFormatException("Error Converting from YAML", e);
            }
        }

        @Override
        public IParser setDontEncodeElements(Set<String> theDontEncodeElements) {
            return jsonParser.setDontEncodeElements(theDontEncodeElements);
        }

        @Override
        public IParser setEncodeElements(Set<String> theEncodeElements) {
            return jsonParser.setDontEncodeElements(theEncodeElements);
        }

        @Override
        public void setEncodeElementsAppliesToChildResourcesOnly(boolean theEncodeElementsAppliesToChildResourcesOnly) {
            jsonParser.setEncodeElementsAppliesToChildResourcesOnly(theEncodeElementsAppliesToChildResourcesOnly);
        }

        @Override
        public boolean isEncodeElementsAppliesToChildResourcesOnly() {
            return jsonParser.isEncodeElementsAppliesToChildResourcesOnly();
        }

        @Override
        public IParser setEncodeForceResourceId(IIdType theForceResourceId) {
            jsonParser.setEncodeForceResourceId(theForceResourceId);
            return this;
        }

        @Override
        public IParser setOmitResourceId(boolean theOmitResourceId) {
            jsonParser.setOmitResourceId(theOmitResourceId);
            return this;
        }

        @Override
        public IParser setParserErrorHandler(IParserErrorHandler theErrorHandler) {
            jsonParser.setParserErrorHandler(theErrorHandler);
            return this;
        }

        @Override
        public void setPreferTypes(List<Class<? extends IBaseResource>> thePreferTypes) {
            jsonParser.setPreferTypes(thePreferTypes);
        }

        @Override
        public IParser setPrettyPrint(boolean thePrettyPrint) {
            jsonParser.setPrettyPrint(thePrettyPrint);
            return this;
        }

        @Override
        public IParser setServerBaseUrl(String theUrl) {
            jsonParser.setServerBaseUrl(theUrl);
            return this;
        }

        @Override
        public IParser setStripVersionsFromReferences(Boolean theStripVersionsFromReferences) {
            jsonParser.setStripVersionsFromReferences(theStripVersionsFromReferences);
            return this;
        }

        @Override
        public IParser setOverrideResourceIdWithBundleEntryFullUrl(
            Boolean theOverrideResourceIdWithBundleEntryFullUrl) {
            jsonParser.setOverrideResourceIdWithBundleEntryFullUrl(theOverrideResourceIdWithBundleEntryFullUrl);
            return this;
        }

        @Override
        public IParser setSummaryMode(boolean theSummaryMode) {
            jsonParser.setSummaryMode(theSummaryMode);
            return this;
        }

        @Override
        public IParser setSuppressNarratives(boolean theSuppressNarratives) {
            jsonParser.setSuppressNarratives(theSuppressNarratives);
            return this;
        }

        /*
        @Override
        public IParser setDontEncodeElements(Collection<String> arg0) {
            jsonParser.setDontEncodeElements(arg0);
            return this;
        }
        *
         */

        @Override
        public IParser setDontStripVersionsFromReferencesAtPaths(String... thePaths) {
            jsonParser.setDontStripVersionsFromReferencesAtPaths(thePaths);
            return this;
        }

        @Override
        public IParser setDontStripVersionsFromReferencesAtPaths(Collection<String> thePaths) {
            jsonParser.setDontStripVersionsFromReferencesAtPaths(thePaths);
            return this;
        }

        @Override
        public Set<String> getDontStripVersionsFromReferencesAtPaths() {
            return jsonParser.getDontStripVersionsFromReferencesAtPaths();
        }
    }

    /**
     * This class takes the names of two files and
     * converts the first existing file to the second (non-existing) file.
     * It uses the extensions of the file to determine which parser to
     * use.
     * @param args  Names of files for conversion operation.
     */
    public static void main(String args[]) {
        FhirContext r4 = FhirContext.forR4();
        IParser p;
        if (args.length != 2) {
            System.err.printf("Usage: java %s inputfile outputfile%n", YamlUtils.class.getName());
            System.exit(5);
        }
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        if (!inputFile.exists()) {
            System.err.printf("File %s does not exist.%n", inputFile);
        }
        String ext = StringUtils.substringAfterLast(inputFile.getName(), ".");
        p = getParser(ext, r4);
        Resource r = null;
        try (FileReader fr = new FileReader(inputFile, StandardCharsets.UTF_8)) {
            r = (Resource) p.parseResource(fr);
        } catch (IOException e) {
            System.err.printf("Cannot read %s.%n", inputFile);
            System.exit(1);
        } catch (DataFormatException e) {
            System.err.printf("Cannot parse %s.%n", inputFile);
            System.exit(2);
        }

        ext = StringUtils.substringAfterLast(outputFile.getName(), ".");
        p = getParser(ext, r4);

        try (FileWriter fw = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
            p.encodeResourceToWriter(r, fw);
        } catch (IOException e) {
            System.err.printf("Cannot write %s.%n", outputFile);
            System.exit(3);
        } catch (DataFormatException e) {
            System.err.printf("Cannot convert %s.%n", outputFile);
            System.exit(4);
        }
    }

    private static IParser getParser(String ext, FhirContext r4) {
        switch (ext.toLowerCase(Locale.ENGLISH)) {
        case "yaml":
            return newYamlParser(r4);
        case "xml":
            return r4.newXmlParser();
        case "json":
            return r4.newJsonParser();
        case "rdf":
            throw new IllegalArgumentException("RDF parser implementation is not yet complete");
        default:
            throw new IllegalArgumentException("Unrecognized format: " + ext);
        }
    }
}
