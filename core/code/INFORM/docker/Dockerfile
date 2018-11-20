FROM openjdk:jdk-alpine
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
#FROM ubuntu:18.04
#RUN apt-get update
#RUN export CLASSPATH=.:postgresql-42.2.5.jar
#ENV CLASSPATH .:postgresql-42.2.5.jar
#CMD java -version # JDBCTest
#RUN export CLASSPATH=.:postgresql-42.2.5.jar:json-simple-1.1.1.jar
RUN javac -cp .:postgresql-42.2.5.jar:json-simple-1.1.1.jar JDBCTest.java
#RUN javac JDBCTest.java
#CMD java JDBCTest
CMD java -cp .:postgresql-42.2.5.jar:json-simple-1.1.1.jar JDBCTest #10.97.26.218
# https://stackoverflow.com/questions/31249112/allow-docker-container-to-connect-to-a-local-host-postgres-database
