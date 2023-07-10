FROM tomcat:9.0.65-jre17-temurin-jammy
RUN useradd -U -d ${CATALINA_HOME} -s /bin/bash tomcat && chown -R tomcat:tomcat ${CATALINA_HOME}
USER tomcat:tomcat
COPY api/target/link-api.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]