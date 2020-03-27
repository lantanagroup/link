FROM ubuntu AS build

WORKDIR /tmp
RUN apt-get update && apt-get install openjdk-11-jdk -y && apt-get install maven -y

WORKDIR /tmp
COPY . .
RUN mvn install

FROM tomcat:9-jre11
COPY --from=build /tmp/target/*.war /usr/local/tomcat/webapps/
EXPOSE 8080
CMD ["catalina.sh", "run"]
