package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.JsonProperties;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BasePrepareQuery implements IPrepareQuery {
    private static final Logger logger = LoggerFactory.getLogger(BasePrepareQuery.class);
    protected JsonProperties properties;
    protected IGenericClient fhirClient;
    protected Map<String, String> criteria;
    protected Map<String, Object> contextData;

    public JsonProperties getProperties() {
        return this.properties;
    }

    public void setProperties(JsonProperties properties) {
        this.properties = properties;
    }

    public IGenericClient getFhirClient() {
        return this.fhirClient;
    }

    public void setFhirClient(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    public Map<String, String> getCriteria() {
        return this.criteria;
    }

    public void setCriteria(Map<String, String> criteria) {
        this.criteria = criteria;
    }

    public Map<String, Object> getContextData() {
        return this.contextData;
    }

    public void setContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
    }

    public Object getContextData(String key) {
        return this.getContextData().get(key);
    }

    public void addContextData(String key, Object data) {
        this.getContextData().put(key, data);
    }

    protected Map<String, Resource> search(String query) {
        Bundle b = rawSearch(query);
        return bundleToMap(b);
    }

    public Bundle rawSearch(String query) {
        try {
            Bundle bundle = fhirClient.search().byUrl(query).returnBundle(Bundle.class).execute();

            logger.debug(this.getClass().getName() + " executing query: " + query);

            return bundle;
        } catch (Exception ex) {
            this.logger.error("Could not retrieve data for " + this.getClass().getName() + ": " + ex.getMessage(), ex);
        }
        return null;
    }

    private Map<String, Resource> bundleToMap(Bundle b) {
        if (b == null) {
            return null;
        }
        HashMap<String, Resource> resMap = new HashMap<String, Resource>();
        List<Bundle.BundleEntryComponent> entryList = b.getEntry();
        for (Bundle.BundleEntryComponent entry : entryList) {
            Resource res = entry.getResource();

            resMap.put(res.getIdElement().getIdPart(), res);
        }
        if (b.getLink(Bundle.LINK_NEXT) != null) {
            // Make sure the next url has the correct base url. Some HAPI implementations don't configure this correctly, and it's easy to work around.
            String nextUrl = b.getLink(Bundle.LINK_NEXT).getUrl();
            nextUrl = nextUrl.substring(nextUrl.indexOf("?"));
            b.getLink(Bundle.LINK_NEXT).setUrl(fhirClient.getServerBase() + nextUrl);

            // Request the next page for the bundle.
            Bundle nextPage = fhirClient.loadPage().next(b).execute();
            resMap.putAll(bundleToMap(nextPage));
        }
        return resMap;
    }
}
