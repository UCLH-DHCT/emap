Notes for testing JDBCTest

1. Find out IP address of en0 on this machine with ifconfig command
	Open a new shell and cd to docker directory
	Edit config.json so both IP addresses in that file are the same as en0's.
	docker build -t jdbctest .

1. Run script to reset entries in INFORM_SCRATCH (i.e. set to empty tables):
	psql -f create_tables.sql INFORM_SCRATCH

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

13. Finally delete the image
	docker image rm -f jdbctest
