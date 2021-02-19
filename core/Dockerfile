FROM maven:3.6.3-jdk-11-slim
# Run commands through bash so source is found
SHELL ["/bin/bash", "-c"]
# Install zip & unzip for glowroot
RUN apt update; apt install -yy zip
# Set up the Maven proxy settings
COPY Emap-Core/set_mvn_proxy.sh /app/
# Download and extract glowroot
WORKDIR /app/Emap-Core
RUN curl -s https://api.github.com/repos/glowroot/glowroot/releases/latest \
  | grep browser_download_url | grep "\.zip" | grep -v "central" | cut -d '"' -f 4 \
  | xargs curl -L -o glowroot.zip; unzip glowroot.zip
COPY Emap-Core/glowroot.properties glowroot/glowroot.properties
COPY Emap-Core/config.json glowroot/config.json
# Copy pom and checkstyle, then generate resources (requires inform-annotations to be installed)
COPY Emap-Interchange/pom.xml /app/Emap-Interchange/
COPY Emap-Interchange/inform-checker.xml /app/Emap-Interchange/
RUN source /app/set_mvn_proxy.sh; mvn dependency:go-offline -f /app/Emap-Interchange/pom.xml
COPY Inform-DB/pom.xml /app/Inform-DB/
COPY Inform-DB/inform-checker.xml /app/Inform-DB/
COPY Inform-DB/inform-db/pom.xml /app/Inform-DB/inform-db/
COPY Inform-DB/inform-annotations/ /app/Inform-DB/inform-annotations/
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true -f /app/Inform-DB/inform-annotations/pom.xml
RUN source /app/set_mvn_proxy.sh; mvn dependency:go-offline -f /app/Inform-DB/pom.xml
COPY Emap-Core/pom.xml /app/Emap-Core/
COPY Emap-Core/inform-checker.xml /app/Emap-Core/
RUN source /app/set_mvn_proxy.sh; mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies -f /app/Emap-Core/pom.xml
# Set up Inform-DB
COPY Inform-DB/inform-db/src/ /app/Inform-DB/inform-db/src/
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true -f /app/Inform-DB/pom.xml
# Set up Emap-Interchange
COPY Emap-Interchange/src/ /app/Emap-Interchange/src/
RUN source /app/set_mvn_proxy.sh; mvn install -f /app/Emap-Interchange/pom.xml
# Set up Emap-Core
WORKDIR /app/Emap-Core
COPY Emap-Core/src/ /app/Emap-Core/src/
# RUN source set_mvn_proxy.sh; mvn -Dspring.profiles.active=test integration-test -Pemapstar
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true -Pemapstar -Dstart-class=uk.ac.ucl.rits.inform.datasinks.emapstar.App
# -Dstart-class=uk.ac.ucl.rits.inform.datasources.ids.AppHl7
CMD ["java", "-javaagent:./glowroot/glowroot.jar", "-jar", "./target/Emap-Core.jar"]
