# nandina

## Building/compiling

### WAR file (for Tomcat deployment)

To build/compile the project, run the following from the root:

```
mvn install
```

### Docker image

To build the docker image, run the following from the root:

```
docker build . --tag nandina:latest
```

This will produce a nandina.war file in the web/target folder.

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
docker run nandina:latest
```

## Configuration

### Properties

| Property | Description |
| -------- | ----------- |
| export.format | "xml" or "json" or "csv" |
| | |
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

# Development

## Building Angular/UI app

The Angular CLI needs to be installed so that you can run `ng` commands from the command-line:

```
npm install -g @angular/cli
```

After the Angular CLI is installed, you need to run `npm ci` to install the Node.JS dependencies, and then `ng build --watch` to compile the angular application's TypeScript files into JS files, that are output to the `src/main/resources/output` directory. These commands need to be run in the `web` directory/project, where the Angular files are stored (`angular.json`, `package.json`, etc.)

## Committing changes to Angular

Be sure to run `ng build --prod` prior to committing your TS changes. This optimizes the compiled TS for production environments. The CI build does not re-compile the TS, it is up to the developer to make sure that the compiled TS is committed the way it should be used in the deployed environments.

## Compiled TS cached by IntelliJ

If you run `ng build --watch` in a separate process from IntelliJ or webstorm, you typically have to re-compile the Java application after NG has recompiled the typescript, otherwise the compiled JS files don't get put in the target/classes/public directory. This is because the default output location for `ng build` is in the `src/main/resources/public` directory, *not* the `target/classes/public` directory.

To get around this issue during development, you can modify your run configuration in IntelliJ and Eclipse to pass the following `VM Option` when debugging/running the server application:

`-Dspring.resources.static-locations=file:/E:/Code/nandina/web/src/main/resources/public/`

This will tell Spring Boot to look for the static files in the same directory that `ng build` outputs them to, and you will no longer have to recompile the java application after you've made changes to the TypeScript that NG re-compiled.