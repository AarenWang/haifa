FROM maven:3.6-jdk-8-slim
COPY src /usr/src/haifa/
COPY pom.xml /usr/src/haifa/
WORKDIR /usr/src/haifa/
RUN mvn  clean package -Dmaven.test.skip=true

FROM tomcat:8.5.40-jre8-slim
COPY target/*.war  /usr/local/tomcat/webapp

