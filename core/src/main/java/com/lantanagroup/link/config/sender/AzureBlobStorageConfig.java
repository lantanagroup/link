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
@ConfigurationProperties(prefix = "sender.abs")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class AzureBlobStorageConfig {

  /***
   <strong>sender.abs.azure-storage-connection-string</strong><br>This issuer is connection string used to connect to an azure blog storage endpoint
   */
  private String azureStorageConnectionString;

  /***
   <strong>sender.abs.azure-sas-token</strong><br>This issued sas token is for access to the azure blog storage container
   */
  private String azureSasToken;

  /***
   <strong>sender.abs.azure-storage-container-name</strong><br>This is the name of the azure blog storage container
   */
  private String azureStorageContainerName;

}
