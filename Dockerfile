FROM maven:latest as builder
WORKDIR /app
RUN git clone https://github.com/Regina117/jenks.git /app
WORKDIR /app/src
# build maven
RUN mvn clean package -DskipTests
FROM tomcat:9-jdk11-openjdk-slim
#install workdir for tomcat
WORKDIR /usr/local/tomcat9/webapps/
#copy war
COPY --from=builder /app/src/web/app/target/*.war ./
RUN rm -rf /usr/local/tomcat9/work/* /usr/local/tomcat9/temp/*
CMD ["catalina.sh", "run"]
