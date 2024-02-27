FROM maven:3.8.5-openjdk-17 AS build

ARG build
ARG version

WORKDIR /tmp

# Copy code and compile
COPY . .

#RUN echo "version: $version\nbuild: $build" > api/src/main/resources/build.yml

WORKDIR /tmp
RUN mvn clean install -pl api -am '-Dmaven.test.skip=true'

FROM tomcat:10.1-jre17-temurin-jammy
RUN useradd -U -d ${CATALINA_HOME} -s /bin/bash tomcat && chown -R tomcat:tomcat ${CATALINA_HOME}
USER tomcat:tomcat
COPY --from=build /tmp/api/target/link-api.war /usr/local/tomcat/webapps/ROOT.war
COPY --from=build /tmp/tomcat-context.xml /usr/local/tomcat/conf/context.xml
EXPOSE 8080
CMD ["catalina.sh", "run"]
