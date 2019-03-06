FROM maven:3.5.4-jdk-8-alpine
WORKDIR /app
# download as much as possible early on,
# only redoing if the pom changes
COPY set_mvn_proxy.sh /app/
COPY pom.xml /app/
RUN source set_mvn_proxy.sh; mvn dependency:resolve
COPY . /app
RUN source set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true
CMD ["java", "-jar", "./target/HL7Processor-1.0-SNAPSHOT.jar"]
