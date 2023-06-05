package com.lantanagroup.link.config.sender;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sender.file")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class FileSystemSenderConfig {
    private String path;
    ;
    private Formats format = Formats.JSON;
    private Boolean pretty = false;

    public enum Formats {
        JSON,
        XML
    }
}
