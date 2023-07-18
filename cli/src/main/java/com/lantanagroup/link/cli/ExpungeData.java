package com.lantanagroup.link.cli;

import com.lantanagroup.link.auth.OAuth2Helper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class ExpungeData extends BaseShellCommand {
    private static final Logger logger = LoggerFactory.getLogger(ExpungeDataConfig.class);

    @ShellMethod(
            key = "expunge-data",
            value = "Call API expunge function to clear data")
    public void execute() {
        ExpungeDataConfig config = applicationContext.getBean(ExpungeDataConfig.class);

        if (!ValidConfiguration(config)) {
            logger.error("Issue with expunge-data configuration...");
            System.exit(1);
        }

        String url = config.getApiUrl() + "/data/expunge";
        logger.info("Calling API Expunge Data at {}", url);

        try {
            String token = OAuth2Helper.getToken(config.getAuth());
            if (token == null) {
                throw new Exception("Authorization failed");
            }
            HttpDelete request = new HttpDelete(url);
            request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));

            HttpResponse response = Utility.HttpExecuter(request, logger);
            logger.info("HTTP Response Code {}", response.getStatusLine().getStatusCode());

        } catch (Exception ex) {
            logger.error("Error calling API Expunge Data - {}", ex.getMessage());
            System.exit(1);
        }

        logger.info("Expunge Data Call Complete");

        System.exit(0);

    }

    private Boolean ValidConfiguration(ExpungeDataConfig config) {
        if (StringUtils.isBlank(config.getApiUrl())) {
            logger.error("Parameter expunge-data.api-url parameter is required.");
            return false;
        }

        if (config.getAuth() == null) {
            logger.error("Parameter expunge-data.auth is required.");
            return false;
        }

        if (config.getAuth().getCredentialMode() == null) {
            logger.error("Parameter expunge-data.auth.credential-mode is required.");
            return false;
        }

        if (!config.getAuth().hasCredentialProperties()) {
            logger.error("Some issue with expunge-data.auth credential properties.");
            return false;
        }

        return true;
    }
}
