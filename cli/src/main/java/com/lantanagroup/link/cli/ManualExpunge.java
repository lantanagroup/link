package com.lantanagroup.link.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.model.ExpungeResourcesToDelete;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.Scanner;

@ShellComponent
public class ManualExpunge extends BaseShellCommand {
    private static final Logger logger = LoggerFactory.getLogger(ManualExpunge.class);

    @ShellMethod(
            key = "manual-expunge",
            value = "Manual expunge lists of Resources from the Data Store")
    public void execute(String resourceType, String[] resourceIdentifiers) {

        ManualExpungeConfig config = applicationContext.getBean(ManualExpungeConfig.class);

        Scanner scanner = new Scanner(System.in);
        System.out.printf("Are You SURE You Want To Delete These %s Items? (type YES to continue) ", resourceType);
        String promptResponse = scanner.next();

        if (!promptResponse.equals("YES")) {
            logger.info("You don't seem to be sure... exiting.");
            System.exit(0);
        }

        String url = config.getApiUrl() + "/data/manual-expunge";
        logger.info("Calling API Manual Expunge Data at {}", url);

        ExpungeResourcesToDelete toDelete = new ExpungeResourcesToDelete();
        toDelete.setResourceType(resourceType);
        toDelete.setResourceIdentifiers(resourceIdentifiers);

        try {
            String token = OAuth2Helper.getToken(config.getAuth());
            if (token == null) {
                throw new Exception("Authorization failed");
            }
            HttpPost request = new HttpPost(url);
            request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
            request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());

            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(toDelete);
            request.setEntity(new StringEntity(payloadJson));

            HttpResponse response = Utility.HttpExecuter(request, logger);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception(String.format("Issue calling Manual Expunge, Response Code: %s", response.getStatusLine().getStatusCode()));
            }

            logger.info("HTTP Response Code {}", response.getStatusLine().getStatusCode());

        } catch (Exception ex) {
            logger.error("Error calling API Manual Expunge Data - {}", ex.getMessage());
            System.exit(1);
        }

        logger.info("Manual Expunge Data Call Complete");

        System.exit(0);

    }
}
