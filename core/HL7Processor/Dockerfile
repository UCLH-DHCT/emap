# Modified from the one at https://codefresh.io/docker-tutorial/java_docker_pipeline/
FROM openjdk:8-jdk-alpine
# ----
# Install Maven
RUN apk add --no-cache curl tar bash
ARG MAVEN_VERSION=3.5.4
ARG USER_HOME_DIR="/root"
RUN mkdir -p /usr/share/maven && \
curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar -xzC /usr/share/maven --strip-components=1 && \
ln -s /usr/share/maven/bin/mvn /usr/bin/mvn
ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"
# speed up Maven JVM a bit
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
#ENTRYPOINT ["/usr/bin/mvn"]
# ----
# Install project dependencies and keep sources
WORKDIR /usr/src/app
# install maven dependency packages (keep in image)
COPY pom.xml pom.xml
#COPY settings.xml $MAVEN_HOME/conf/settings.xml
RUN mvn install && rm -rf target
#RUN mkdir -p $MAVEN_HOME/conf
##COPY settings.xml settings.xml
# copy other source files (keep in image)
COPY src src
COPY config.json config.json
RUN mvn compile
#RUN mvn package
RUN mvn help:effective-settings
#CMD mvn exec:java -Dexec.mainClass="uk.ac.ucl.rits.inform.HL7Processor"
#CMD mvn org.codehaus.mojo:exec-maven-plugin:1.6.0:java -Dexec.mainClass="uk.ac.ucl.rits.inform.HL7Processor"
CMD ["mvn", "exec:java", "-Dexec.mainClass=uk.ac.ucl.rits.inform.HL7Processor"]

