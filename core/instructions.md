# How to use HL7Processor out of the box.

## Using it in its real environment
NOTE: you will almost certainly NOT wish to do this until you have tried it out on your own machine (see below for instructions).
If you want to use HL7Processor on the real GAE, having done a `git clone` you will need to:<br>

(a) edit (or create) a `config.json` file in the `HL7Processor` directory. A template can be found in `example_config.json`. Note that you will probably not wish to use the `debug` option, so either remove that line (if it exists) or change it to  
<br>`"debugging":"false"`
<br>
You will then need to edit the `config.json` file to have the IP addresses, usernames and passwords required for access to the IDS and UDS.

(b) Execute the commands:<br>
`docker-compose build`<br>
`docker-compose up`


## Using it on your own machine
### 1. Set up Postgres databases
This will require you to set up Postgres databases to simulate the IDS and UDS. The IDS (Immutable Data Store) 
is read-only and will store the raw HL7 messages which Atos put there. Atos perform some parsing of the messages so that certain data items are stored as columns in the database master table. There is also a "merge" table to keep track of patient merges.

In my case, on my MacBook Pro, I installed [PostgresAppServer](https://postgresapp.com) as a lightweight way of running postgres databases on my machine. 

You then need to create the simulated IDS and UDS. You can call them what you like; in this example
the IDS is called `DUMMY_IDS` and the UDS `INFORM_SCRATCH`. 

Once you have created the empty databases, scripts are provided to insert and update the tables.

The SQL scripts required reside in the toplevel `scripts` directory.

To set up the tables in the UDS, type: `psql -f create_dummy_uds.sql INFORM_SCRATCH`. <br>

To set up the IDS tables, and populate with two messages, execute `psql -f create_dummy_ids.sql DUMMY_IDS`. 


You can then connect to the IDS and verify that there are indeed two records in the table. <br>

To connect: `psql DUMMY_IDS` <br>

Once connected, the command ` \z` lists the tables. You can then get useful information by issuing the following two commands:<br>
         `select * from tbl_ids_master limit 1;` <br>
         `select unid, patientname, patientsurname, hospitalnumber, admissiondate, dischargedate, messagedatetime from tbl_ids_master;` <br>

Note that there are not any discharge dates yet

To exit, type `\q`.

You can also check that there is nothing in the UDS tables by connecting: `psql INFORM_SCRATCH`


### 2. Configuration and docker
The `HL7Processor` program reads in parameters from `config.json`. To run with docker a copy of this must
reside in the `HL7Processor/docker` directory. A template can be found in `HL7Processor/example_config.json`. You need to state the username and passwords you used to set up the databases. If you do not have a password, the password string will simply be `""` in the config file.

As you will be running the IDS and UDS on your own machine, the "idshost" and "udshost" values are likely to be `host.docker.internal`. If that doesn't work, try `127.0.0.1`.Or you can try the en0 value for IP address you get by using the `ifconfig` command.
Now say<br>
	`docker-compose build`<br>
	`docker-compose up`<br>

You should then find there are two people added to PERSON_SCRATCH table in UDS, and two live hospital visits in the PATIENT_VISIT table.<br>
	`psql INFORM_SCRATCH`
	`select * from person_scratch;`
	`select visitid, hospitalnumber, patientlocation, admissiondate, dischargedate, lastupdated from patient_visit;`

In this implementation, the PATIENT_VISIT table summarises the visit (start and end) to the hospital. However, the BEDVISIT table keeps track of a patient as they move around between beds *within* the hospital. Thus if we issue an HL7 transfer message (ADT^A02 - see step 4 below) the patient will still be in the same PATIENT_VISIT entry, but they will have a new BED_VISIT entry to show that they have finished being in one location (which can be an individual bed) and have started being in another. 

### 3. Add a third admission message to the IDS
Type:
	`psql -f insert_admit_message.sql DUMMY_IDS`
	Check this record has been added:
	select unid, patientname, patientsurname, hospitalnumber, admissiondate, dischargedate, messagedatetime from tbl_ids_master;

 Run container again
	`docker-compose up`

If you now log into UDS you can check there are 3 entries:
	psql INFORM_SCRATCH
	> select name, last_update_date_time from person;
	> select * from patient_visit;
	Note that the last update datetime is that of the HL7 message in the IDS, NOT its PersistDateTime 
	(time when written to db)

	There should be 3 current hospital visits and 3 people.

### 4. Transfer patient Leonardo Da Vinci from one bed to another
Type: `psql -f insert_transfer_message.sql DUMMY_IDS`.

If you log in to the IDS you ill see there should be 4 HL7 messages.

To run the Java code again, which will read the IDS and update the UDS, type:<br>
	`docker-compose up`

If you now log in to the UDS you will see that the bedvisit table has two entries for the same patient visit, corresponding to the visit of Leonardo.

### 5. Discharge patient from hospital
Type:
	`psql -f insert_discharge_message.sql DUMMY_IDS`

	Now run docker image again:
	docker-compose up

 Open dummy UDS and you should find there are still 3 people but Leonardo's visit has come to an end.
	`psql DUMMY_UDS` <br>
	`> select * from PERSON_SCRATCH;` <br>
	`> select * from patient_visit;` <br>
	`> \q`    -- to close database

Running the container again, without updating the IDS, will have no effect on the UDS.

### 6. Other tests
You can insert a patient with null hospital number to the IDS:
`psql -f insert_nullhn_admit_message.sql DUMMY_IDS`
If you then run the container, you will see that the UDS does NOT get updated. This is because it uses
hospital number as a primary key in the database.


### 7. Tidy up docker image
  `docker-compose down` <br>
  `docker-compose stop`

