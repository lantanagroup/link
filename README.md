# flintlock

## Building/compiling

### WAR file (for Tomcat deployment)

To build/compile the project, run the following from the root:

```
mvn install
```

### Docker image

To build the docker image, run the following from the root:

```
docker build . --tag flintlock:latest
```

This will produce a flintlock.war file in the web/target folder.

### Angular Web App (the UI)

The angular project is pre-compiled for production use. The compiled HTML/JS files are stored in the web/src/main/resources/public directory; this is the default directory that Spring uses for static HTML pages.

During development, you can use the following to build the angular app (and watch for changes) in real-time:

```
# from the "web" directory
ng build --watch
``` 

Note: If debugging using Eclipse/IntelliJ, whenever angular changes are detected and recompiled, the

When ready to commit changes to the UI, run the following:

```
# from the web directory
ng build --prod
```  

## Running via Docker

To run the docker image:

```
docker run flintlock:latest
```

## Configuration

### Properties

| Property | Description |
| -------- | ----------- |
| fhirServer.base | The base URL of the FHIR server |
| fhirServer.username | If basic authentication is required for the FHIR server, the username to authenticate with the FHIR server. |
| fhirServer.password | If basic authentication is required for the FHIR server, the password to authenticate with the FHIR server. |
| fhirServer.bearerToken | If a Bearer token is supported (such as an API key), the token value (without "Bearer " in it) to use to authenticate against the FHIR server. |
| | |
| auth.issuer | The URL of the issuing authentication provider. |
| auth.jwksUrl | The full URL to the authentication provider's JWKS to validate authentication tokens. |
| auth.clientId | The client id of the app in the authentication provider. |
| auth.scope | The scopes to request from the authentication provider. Suggest a minimum of `openid profile email`. |
| | |
| query.hospitalized | The class to use for the Hospitalized query. |
| query.hospitalizedAndVentilated | The class to use for the Hospitalized & Ventilated query. |
| query.hospitalOnset | The class to use for the Hospital Onset query. |
| query.edOverflow | The class to use for the ED/Overflow query. |
| query.edOverflowAndVentilated | The class to use for the ED/Overflow and Ventilated query. |
| query.deaths | The class to use for the Deaths query. |
| | |
| terminology.covidCodes | The comma-separated list of codes that represent COVID-19 for the environment. |
| terminology.deviceTypeCodes | The comma-separated list of codes that represent the ventilation devices for the environment. |

### Customizing

All configurable properties are located in `application.properties`. To override the configurable properties without touching the original `application.properties` file:

1. Copy the `application.properties` file and rename to application-local.properties. Set an environment variable for `SPRING_CONFIG_NAME=application-local`.
2. Use the `SPRING_CONFIG_LOCATION=file:///some/directory` environment variable, where the directory contains `application.properties` or the `SPRING_CONFIG_NAME`.

See [this article](https://www.baeldung.com/spring-properties-file-outside-jar) for more information on how to specify a customized `application.properties` file for the Spring web application.

## Version Updates

To update the version number for the modules in the project run the following:

```
mvn versions:set -DnewVersion=2.50.1-SNAPSHOT
mvn versions:revert
mvn versions:commit
```