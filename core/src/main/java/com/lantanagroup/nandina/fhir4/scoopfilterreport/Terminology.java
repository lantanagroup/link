package com.lantanagroup.nandina.fhir4.scoopfilterreport;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class Terminology {
	

	protected static final Logger logger = LoggerFactory.getLogger(Terminology.class);
	private FhirContext ctx = FhirContext.forR4();
	private IParser xmlParser = ctx.newXmlParser();
	

    private Set<String> valueSetToSet(ValueSet vs){
    	Set<String> set = new HashSet<String>();
    	if (vs.getExpansion().hasContains()) {
        	for (ValueSetExpansionContainsComponent contains: vs.getExpansion().getContains()) {
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
        	for (ValueSetExpansionContainsComponent childContains: contains.getContains()) {
        		Set<String> c = extractContainsSet(childContains);
        		set.addAll(c);
        	}
    	}
		return set;
	}
    

    private Map<String,Set<String>> codeSets = new HashMap<String,Set<String>>();
    
    public Set<String> getValueSetAsSetString(String oid){
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
