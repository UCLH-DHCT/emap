#!/bin/sh
#
# Generate documentation for the HL7 Java code.

mkdir -p docs/JDBCTest

CP="."
for i in ../../hapi-dist-2/lib/*.jar;
do
		  CP=$i:$CP;
done

javadoc -d docs -private -cp "src:$CP" uk.ac.ucl.rits.inform Consumer.java Engine.java

javadoc -cp .:json-simple-1.1.1.jar:postgresql-42.2.5.jar  -private -d docs/JDBCTest JDBCTest.java

