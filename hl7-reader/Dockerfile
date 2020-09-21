FROM maven:3.6.3-jdk-11-slim
# Run commands through bash so source is found
SHELL ["/bin/bash", "-c"]

WORKDIR /app
COPY emap-hl7-processor/set_mvn_proxy.sh /app/
COPY Emap-Interchange/ /app/Emap-Interchange/
WORKDIR /app/Emap-Interchange
RUN source /app/set_mvn_proxy.sh; mvn install
WORKDIR /app/emap-hl7-processor
COPY emap-hl7-processor/pom.xml /app/emap-hl7-processor/
RUN source /app/set_mvn_proxy.sh; mvn dependency:resolve
COPY emap-hl7-processor/. /app/emap-hl7-processor/
#RUN source set_mvn_proxy.sh; mvn -Dspring.profiles.active=test integration-test -Phl7source
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true -Dstart-class=uk.ac.ucl.rits.inform.datasources.ids.AppHl7
CMD ["java", "-jar", "./target/HL7Processor.jar"]
