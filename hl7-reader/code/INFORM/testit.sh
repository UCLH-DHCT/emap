#!/bin/sh
# *******************************************
# * This batch file runs example code
# *
# * For more info, see: http://hl7api.sourceforge.net/devbyexample.html
# ******************************************* 

CP="."
for i in ../../hapi-dist-2/lib/*.jar;
do
		  CP=$i:$CP;
done

#CP=.:postgresql-42.2.5.jar:json-simple-1.1.1.jar:junit-4.13-beta-1.jar:hamcrest-core-2.1.jar:$CP
CP=.:postgresql-42.2.5.jar:json-simple-1.1.1.jar:junit-4.10.jar:$CP

echo "Removing old class files"
rm src/uk/ac/ucl/rits/inform/*.class HL7Processor.class

javac -classpath $CP ./src/uk/ac/ucl/rits/inform/Engine.java HL7Processor.java TestHL7Processor.java TestRunner.java

java -cp "src:$CP" TestRunner
