FROM openjdk:jdk-alpine
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
#FROM ubuntu:18.04
#RUN apt-get update
#RUN export CLASSPATH=.:postgresql-42.2.5.jar
#ENV CLASSPATH .:postgresql-42.2.5.jar
#CMD java -version # JDBCTest
CMD java -cp .:postgresql-42.2.5.jar JDBCTest
