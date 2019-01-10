#!/bin/sh
# *******************************************
# * This batch file runs our unit tests
# *
# ******************************************* 

CP="."
for i in ../../../hapi-dist-2/lib/*.jar;
do
		  CP=$i:$CP;
done

CP=.:../postgresql-42.2.5.jar:../json-simple-1.1.1.jar:$CP

#CP=junit-4.10.jar:$CP
#CP=junit-4.13-beta-1.jar:hamcrest-core-2.1.jar:$CP
CP=junit-4.12.jar:hamcrest-2.1.jar:$CP #hamcrest-core-2.1.jar:hamcrest-all-1.3.jar:$CP

CP=mockito-core-2.23.13.jar:$CP

# Mockito dependencies:
CP=objenesis-2.6.jar:byte-buddy-agent-1.9.0.jar:byte-buddy-1.9.0.jar:$CP


echo "Removing old class files"
#rm ../src/uk/ac/ucl/rits/inform/*.class #HL7Processor.class
rm *.class

echo "Compiling"
javac -classpath $CP ../src/uk/ac/ucl/rits/inform/Engine.java ../src/uk/ac/ucl/rits/inform/HL7Processor.java TestConvertTimestamp.java TestNull.java JunitTestSuite.java TestRunner.java

echo "Running tests"
java -cp "../src:$CP" TestRunner
