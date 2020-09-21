FROM maven:3.5.4-jdk-8-alpine
WORKDIR /app
COPY Emap-Core/set_mvn_proxy.sh /app/
COPY Emap-Interchange/ /app/Emap-Interchange/
WORKDIR /app/Emap-Interchange
RUN source /app/set_mvn_proxy.sh; mvn install
COPY Inform-DB/ /app/Inform-DB/
WORKDIR /app/Inform-DB
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true
WORKDIR /app/Emap-Core
COPY Emap-Core/pom.xml /app/Emap-Core/
RUN source /app/set_mvn_proxy.sh; mvn dependency:resolve -Pemapstar
COPY Emap-Core/. /app/Emap-Core/
#RUN source set_mvn_proxy.sh; mvn -Dspring.profiles.active=test integration-test -Pemapstar
RUN source /app/set_mvn_proxy.sh; mvn install -Dmaven.test.skip=true -Pemapstar -Dstart-class=uk.ac.ucl.rits.inform.datasinks.emapstar.App
# -Dstart-class=uk.ac.ucl.rits.inform.datasources.ids.AppHl7
CMD ["java", "-jar", "./target/EmapCore.jar"]
