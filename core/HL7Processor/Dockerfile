FROM maven:3.5.4-jdk-8-alpine
RUN apk update && apk add git
WORKDIR /app
# download as much as possible early on,
# only redoing if the pom changes
COPY set_mvn_proxy.sh /app/
RUN git clone --branch more_demo https://github.com/inform-health-informatics/Inform-DB.git
WORKDIR /app/Inform-DB
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true
WORKDIR /app
COPY pom.xml /app/
RUN source /app/set_mvn_proxy.sh; mvn dependency:resolve
COPY . /app
#RUN source set_mvn_proxy.sh; mvn -Dspring.profiles.active=test integration-test
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true
CMD ["java", "-jar", "./target/HL7Processor-1.0-SNAPSHOT.jar"]
