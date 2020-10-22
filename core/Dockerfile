FROM maven:3.6.3-jdk-11-slim
# Run commands through bash so source is found
SHELL ["/bin/bash", "-c"]
# Install zip & unzip for glowroot
RUN apt update; apt install -yy zip
WORKDIR /app
# Set up the Maven proxy settings
COPY Emap-Core/set_mvn_proxy.sh /app/
# Set up Emap-Interchange
COPY Emap-Interchange/ /app/Emap-Interchange/
WORKDIR /app/Emap-Interchange
RUN source /app/set_mvn_proxy.sh; mvn install
# Set up Inform-DB
COPY Inform-DB/ /app/Inform-DB/
WORKDIR /app/Inform-DB
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true
# Set up Emap-Core
WORKDIR /app/Emap-Core
# Download and extract glowroot
RUN curl -s https://api.github.com/repos/glowroot/glowroot/releases/latest | grep browser_download_url | grep "\.zip" | grep -v "central" | cut -d '"' -f 4 | xargs curl -L -o glowroot.zip; unzip glowroot.zip
COPY Emap-Core/glowroot.properties glowroot/glowroot.properties
COPY Emap-Core/pom.xml /app/Emap-Core/
RUN source /app/set_mvn_proxy.sh; mvn dependency:resolve -Pemapstar
COPY Emap-Core/. /app/Emap-Core/
# RUN source set_mvn_proxy.sh; mvn -Dspring.profiles.active=test integration-test -Pemapstar
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true -Pemapstar -Dstart-class=uk.ac.ucl.rits.inform.datasinks.emapstar.App
# -Dstart-class=uk.ac.ucl.rits.inform.datasources.ids.AppHl7
CMD ["java", "-javaagent:./glowroot/glowroot.jar", "-jar", "./target/Emap-Core.jar"]
