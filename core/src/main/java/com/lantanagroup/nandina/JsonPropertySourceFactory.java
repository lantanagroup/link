package com.lantanagroup.nandina;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.util.Map;

public class JsonPropertySourceFactory implements PropertySourceFactory {
    private static final Logger logger = LoggerFactory.getLogger(JsonPropertySourceFactory.class);

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        String fileConfig = System.getProperty("config.file");
        String resourceConfig = System.getProperty("config.resource");

        Map readValue = null;
        if (resourceConfig != null) {
            logger.info("Loading config from resource: " + resourceConfig);
            EncodedResource envResource = new EncodedResource(new ClassPathResource(resourceConfig));
            readValue = new ObjectMapper().readValue(envResource.getInputStream(), Map.class);
        } else if (null != fileConfig) {
            logger.info("Loading config from file: " + fileConfig);
            EncodedResource envResource = new EncodedResource(new FileSystemResource(fileConfig));
            readValue = new ObjectMapper().readValue(envResource.getInputStream(), Map.class);
        } else {
            readValue = new ObjectMapper().readValue(resource.getInputStream(), Map.class);
        }

        return new MapPropertySource("json-property", readValue);
    }
}