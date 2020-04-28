FROM ubuntu AS build

WORKDIR /tmp
RUN apt-get update && apt-get install openjdk-11-jdk -y && apt-get install maven -y

WORKDIR /tmp

# Copy code and compile
COPY . .
RUN mvn install

FROM tomcat:9-jre11
RUN rm -r /usr/local/tomcat/webapps/ROOT
COPY --from=build /tmp/web/target/nandina.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
