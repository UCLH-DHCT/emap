FROM maven:3.6.3-jdk-15-slim
# Run commands through bash so source is found
SHELL ["/bin/bash", "-c"]
# Install zip & unzip for glowroot
RUN apt update; apt install -yy zip
# Set up the Maven proxy settings
COPY emap-hl7-processor/set_mvn_proxy.sh /app/
# Download and extract glowroot
WORKDIR /app/emap-hl7-processor
RUN curl -s https://api.github.com/repos/glowroot/glowroot/releases/latest \
  | grep browser_download_url | grep "\.zip" | grep -v "central" | cut -d '"' -f 4 \
  | xargs curl -L -o glowroot.zip; unzip glowroot.zip
COPY emap-hl7-processor/glowroot.properties glowroot/glowroot.properties
COPY emap-hl7-processor/config.json glowroot/config.json
# Copy pom and checkstyle, then all generate resources
COPY Emap-Interchange/inform-checker.xml /app/Emap-Interchange/
COPY Emap-Interchange/pom.xml /app/Emap-Interchange/
RUN source /app/set_mvn_proxy.sh; mvn dependency:go-offline -f /app/Emap-Interchange/pom.xml
COPY emap-hl7-processor/inform-checker.xml /app/emap-hl7-processor/
COPY emap-hl7-processor/pom.xml /app/emap-hl7-processor/
RUN source /app/set_mvn_proxy.sh; mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies -f /app/emap-hl7-processor/pom.xml
# Install Emap-Interchange
COPY Emap-Interchange/src/ /app/Emap-Interchange/src/
RUN source /app/set_mvn_proxy.sh; mvn install -f /app/Emap-Interchange/pom.xml
# Install hl7-processor
WORKDIR /app/emap-hl7-processor
COPY emap-hl7-processor/src/ /app/emap-hl7-processor/src/
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true -Dstart-class=uk.ac.ucl.rits.inform.datasources.ids.AppHl7
CMD ["java",  "-javaagent:./glowroot/glowroot.jar", "-jar", "./target/HL7Processor.jar"]
