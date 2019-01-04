FROM openjdk:jdk-alpine
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
#FROM ubuntu:18.04
#RUN apt-get update
#RUN export CLASSPATH=.:postgresql-42.2.5.jar
#ENV CLASSPATH .:postgresql-42.2.5.jar
#CMD java -version # JDBCTest
#RUN export CLASSPATH=.:postgresql-42.2.5.jar:json-simple-1.1.1.jar
RUN javac -cp .:src/:postgresql-42.2.5.jar:json-simple-1.1.1.jar:hapi-base-2.3.jar:hapi-structures-v27-2.3.jar JDBCTest.java
#RUN javac JDBCTest.java
#CMD java JDBCTest
CMD java -cp .:src:postgresql-42.2.5.jar:json-simple-1.1.1.jar:hapi-base-2.3.jar:hapi-structures-v27-2.3.jar:log4j-1.2.17.jar:slf4j-log4j12-1.7.10.jar:slf4j-api-1.7.10.jar JDBCTest
# https://stackoverflow.com/questions/31249112/allow-docker-container-to-connect-to-a-local-host-postgres-database
