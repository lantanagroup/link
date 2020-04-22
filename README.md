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

## Version Updates

To update the version number for the modules in the project run the following:

```
mvn versions:set -DnewVersion=2.50.1-SNAPSHOT
mvn versions:revert
mvn versions:commit
```