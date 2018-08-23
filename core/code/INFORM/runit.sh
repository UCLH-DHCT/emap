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

rm uk/ac/ucl/rits/inform/*.class

#java -cp $CP ca.uhn.hl7v2.examples.CreateAMessage
javac -classpath $CP -d . Consumer.java

OPTIONS="-f AllMessages.txt"
#OPTIONS = "-h"

java -cp $CP uk.ac.ucl.rits.inform.Consumer $OPTIONS
