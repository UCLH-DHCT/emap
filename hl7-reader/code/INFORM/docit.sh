#!/bin/sh
#
# Generate documentation for the HL7 Java code.

mkdir -p docs/HL7Processor

CP="."
for i in ../../hapi-dist-2/lib/*.jar;
do
		  CP=$i:$CP;
done

javadoc -d docs -private -cp ".:json-simple-1.1.1.jar:postgresql-42.2.5.jar:src:$CP" uk.ac.ucl.rits.inform Consumer.java Engine.java HL7Processor.java

#javadoc -cp .:json-simple-1.1.1.jar:postgresql-42.2.5.jar  -private -d docs/HL7Processor HL7Processor.java

