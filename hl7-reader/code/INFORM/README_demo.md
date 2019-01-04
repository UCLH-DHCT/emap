Notes for testing JDBCTest

To run JDBCTest, use the ./runit.sh script.

To run JDBCTest as a dockerised container, the following jar files need to be copied into the docker directory:

hapi-base-2.3.jar		json-simple-1.1.1.jar		slf4j-api-1.7.10.jar
hapi-hl7overhttp-2.3.jar	log4j-1.2.17.jar		slf4j-log4j12-1.7.10.jar
hapi-structures-v27-2.3.jar	postgresql-42.2.5.jar


We should see the person_scratch, patient_visit and bedvisit tables in the UDS being updated
The bedvisit tables keeps track of a patient as they move around different beds.
So for a single visit, there can be multiple bedvisits.

#The postgres and json ones are in the HL7_java/code/INFORM directory.
Download postgresql-42.2.5.jar from https://jdbc.postgresql.org/download.html
Download json-simple-1.1.1.jar from various places.
The hash I have for my json-simple-1.1.1.jar file is f1535657ebe122f89bad3f75a2dcedad for MD5
The others are in the HL7_java/hapi-dist-2/lib directory. 

0. 	Open a new shell and cd to docker directory
	Edit config.json so the IP addresses etc. point to the ids and uds. If running this on your
own laptop rather than the GAE the address will likely be that of en0 from the ifconfig command. Or 
you can set them both to "host.docker.internal"
	docker build -t jdbctest .

1. If you wish to reset entries in INFORM_SCRATCH (i.e. set to empty tables) add this to config file:
	change 
	"debugging":"false"
	to
	"debugging":"true"

2. Delete entries in DUMMY_IDS and add first two back in:
	psql -f create_dummy_ids.sql DUMMY_IDS

3. In a new window log in to IDS and see there are two messages, one each for two people. No discharge dates.
	psql DUMMY_IDS
	\z lists the tables
	select * from tbl_ids_master limit 1;
	select unid, patientname, patientsurname, hospitalnumber, admissiondate, dischargedate, messagedatetime from tbl_ids_master;

4. In separate window check nothing in UDS yet:
	psql INFORM_SCRATCH

5. Run docker image:
	docker run jdbctest

	You should then find there are two people added to PERSON table in UDS, and two live hospital visits.
	select * from person;
	select visitid, hospitalnumber, patientlocation, admissiondate, dischargedate, lastupdated from patient_visit;

6. Add a third record to IDS. 
	psql -f insert_record_to_dummy_ids.sql DUMMY_IDS
	Check this record has been added:
	select unid, patientname, patientsurname, hospitalnumber, admissiondate, dischargedate, messagedatetime from tbl_ids_master;

7. Run container again
	docker run jdbctest

8. log into UDS and check 3 entries:
	psql INFORM_SCRATCH
	> select name, last_update_date_time from person;
	> select * from patient_visit;
	Note that the last update datetime is that of the HL7 message in the IDS, NOT its PersistDateTime 
	(time when written to db)

	There should be 3 current hospital visits and 3 people.

9. Now add a discharge message to the IDS:
	psql -f insert_discharge_message.sql DUMMY_IDS

	You can check there are now 4 messages in the IDS:
	select unid, patientname, patientsurname, hospitalnumber, admissiondate, dischargedate, messagedatetime from tbl_ids_master;

10.	Now run docker image again:
	docker run jdbctest

11. Open dummy UDS and you should find there are still 3 people but Leonardo's visit has come to an end.
	psql DUMMY_IDS
	> select * from PERSON;
	> select * from patient_visit;
	> \q    -- to close database

	NB steps 8 to 11 can be run multiple times but it will just show different visits for Leonardo.

12. If the container is run multiple times now you should find that the UDS is not updated.

13. You can also run all is OK with the following commands, then running the docker image and
checking each stage, all for Leonardo:

psql -f insert_admit_message.sql DUMMY_IDS
psql -f insert_transfer_message.sql DUMMY_IDS
psql -f insert_discharge_message.sql DUMMY_IDS

There is also a script to insert a patient with null value for hospital ID into the IDS:
psql -f insert_nullhn_admit_message.sql DUMMY_IDS
As the JDBCTest program uses hospitalnumber as the primary key you should find that no new records are
added to the UDS if you then run the docker instance.


14. Finally delete the image
	docker image rm -f jdbctest

==========================
Alternative commands for docker - replace above where needed:

docker-compose build		imnstead of docker build
docker-compose up			instead of docker run jdbctest
docker-compose down
docker-compose stop			instead of docker rm
