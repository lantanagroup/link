FROM node AS build

WORKDIR /tmp

RUN apt-get update && apt-get install default-jdk -y && apt-get install maven -y
RUN npm install -g @angular/cli

WORKDIR /tmp

# Copy code and compile
COPY . .

WORKDIR /tmp/web
RUN npm ci
RUN ng build --prod
WORKDIR /tmp
RUN mvn install

FROM tomcat:9-jre11
RUN rm -r /usr/local/tomcat/webapps/ROOT
COPY --from=build /tmp/web/target/nandina.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
