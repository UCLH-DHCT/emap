Notes for testing JDBCTest.

NB the demo was origionally run on my laptop with a handful of test messages. 
Now the real messages will be coming in.

DO NOT FOLLOW COMMENTED-OUT STEPS!

To run JDBCTest as a dockerised container, the following jar files need to be copied into the docker directory:

hapi-base-2.3.jar		json-simple-1.1.1.jar		slf4j-api-1.7.10.jar
hapi-hl7overhttp-2.3.jar	log4j-1.2.17.jar		slf4j-log4j12-1.7.10.jar
hapi-structures-v27-2.3.jar	postgresql-42.2.5.jar

The postgres and json ones are in the HL7_java/code/INFORM directory on the parsing branch. 
Or you can download them from the web.
The others are in the HL7_java/hapi-dist-2/lib directory. 

0. 	Open a new shell and cd to docker directory
	copy the config.json file from the parent directory and edit:
	Edit config.json so the IP addresses etc. point to the ids and uds. If running this on your
own laptop rather than the GAE the address will likely be that of en0 from the ifconfig command.
	#docker build -t jdbctest .
	docker-compose build

	hopefully this will build OK.

	you can then start running the container
	docker-compose up

	we should see the person_scratch, patient_visit and bedvisit tables in the UDS being updated
	The bedvisit tables keeps track of a patient as they move around different beds.
	So for a single visit, there can be multiple bedvisits.

I originally wrote this script to run on my laptop with specific test HL7 messages. See Github for older
versions. But now we are (hopefully) using the live stream what we see will be different...? 
