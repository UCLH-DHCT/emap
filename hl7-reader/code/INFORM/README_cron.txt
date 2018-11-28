Notes for testing the crontab auto update of the UDS

1. Delete UNID.json

2. Run script to reset entries in INFORM_SCRATCH (i.e. set to empty tables):
	psql -f create_tables.cmd INFORM_SCRATCH

3. Delete entries in DUMMY_IDS and add first two back in:
	psql -f create_dummy_ids.cmd DUMMY_IDS

4. Start cron process which runs the JDBCTest Java file every minute:
	crontab mycrontabfile
   Obviously you will have to edit that for your set up.
   You can check it is running with:
	crontab -l

5. After ~ 1 minute UNID.json should appear with content {"unid":2}

6. Add a third record to IDS. We want to check the cron job updates the UDS with this:
	psql -f insert_record_to_dummy_ids.cmd DUMMY_IDS

7. After ~ 1 minute UNID.json value should change from 2 to 3.

8. log into UDS and check 3 entries:
	psql INFORM_SCRATCH
	> select name, last_update_date_time from person;
	Note that the last update datetime is that of the HL7 message in the IDS, NOT its PersistDateTime 
	(time when written to db)

9. Wait a minute or two and check no duplicate entries by running select command again

	To close database:
	> \q

10. Open dummy IDS and check there are 3 entries. We can look at the persistdatetime to see the times at which the 
    messages were inserted to the IDS. We should find the first 2 are very close and the third is about a minute later:
	psql DUMMY_IDS
	> select patientname, persistdatetime from tbl_ids_master;
	> \q

11. Finally kill the crontab:
	crontab -r
