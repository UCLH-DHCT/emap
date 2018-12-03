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

CP=.:postgresql-42.2.5.jar:json-simple-1.1.1.jar:$CP

#CP="src/uk/ac/ucl/rits/inform/":$CP;

echo "Removing old class files"
rm src/uk/ac/ucl/rits/inform/*.class JDBCTest.class

#java -cp $CP ca.uhn.hl7v2.examples.CreateAMessage
#javac -classpath $CP -d . Consumer.java
javac -classpath $CP ./src/uk/ac/ucl/rits/inform/Engine.java JDBCTest.java

OPTIONS="-f AllMessages.txt"
#OPTIONS="-f Messages-Batch2.txt"
#OPTIONS = "-h"

java -cp "src:$CP" JDBCTest
#java -cp "src:$CP" uk.ac.ucl.rits.inform.Consumer $OPTIONS
#java -cp $CP Consumer $OPTIONS
