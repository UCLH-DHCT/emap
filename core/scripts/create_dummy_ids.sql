/*Assuming IDS database is already created which has  a new user or postgres user.*/

DROP TABLE IF EXISTS TBL_IDS_MASTER;

/*Create table TBL_IDS_MASTER*/
CREATE TABLE TBL_IDS_MASTER (
        UNID SERIAL PRIMARY KEY,
        PatientName varchar(25) ,
        PatientMiddleName varchar(25) ,
        PatientSurname varchar(25) ,
        DateOfBirth timestamp, ----varchar(25) ,
        NHSNumber varchar(25) ,
        HospitalNumber varchar(25) ,
        PatientClass varchar(25) ,
        PatientLocation varchar(25) ,
        AdmissionDate timestamp, ----varchar(25) ,
        DischargeDate timestamp, ---varchar(25) ,
        MessageType varchar(25) ,
        SenderApplication varchar(25) NOT NULL,
        MessageIdentifier varchar(50) NOT NULL,
        MessageFormat varchar(10) NOT NULL,
        MessageVersion varchar(25) NOT NULL,
        MessageDateTime timestamp NOT NULL, ----varchar(25) NOT NULL,
        HL7Message text NOT NULL,
        PersistDateTime timestamp NOT NULL --DEFAULT now()
);

DROP TABLE IF EXISTS TBL_IDS_PATIENTMERGE;

/*Create table TBL_IDS_PATIENTMERGE*/
CREATE TABLE TBL_IDS_PATIENTMERGE (
        UNID SERIAL  PRIMARY KEY,
        PatientId varchar(25) ,
        PreviousPatientId varchar(25) 

);

/* Insert query. */
INSERT INTO TBL_IDS_MASTER (
        PatientName,
        PatientMiddleName,
        PatientSurname,
        DateOfBirth,
        NHSNumber,
        HospitalNumber,
        PatientClass,
        PatientLocation,
        AdmissionDate,
        DischargeDate,
        MessageType,
        SenderApplication,
        MessageIdentifier,
        MessageFormat,
        MessageVersion,
        MessageDateTime,
        HL7Message,
        PersistDateTime)
        VALUES(
        'Fred',
        '',
        'O''Bloggs',
        '2009-03-04', --'20090304',
        '',
        '94006000', -- NB the id below is different
        'I',
        'T11S^B11S^T11S-32',
        '2009-02-11 00:22:00', --200902110022,
        NULL,
        'ADT^A01',
        'ADTOUT',
        'PLW21216519415491111',
        'HL7',
        '2.2',
        '2009-02-11 00:22:00', ---'200902110022', 
'MSH|^~\&|ADTOUT|PLW|||200902110022||ADT^A01|PLW21216519415491111|P|2.2|||AL|NE'
|| chr(10)
|| 'EVN|P01|200902110022||REFAD|U451777^DELL^RIMA'
|| chr(10)
|| 'PID||1234567890|94006000^^^MRENTR^MEDREC||Bloggs^Fred^^^MASTER||200903040302|M|Chang|Z1|35 Gilbert STREET^^LONDON^^KK2 9LU||020 7355 7255|Not Recorded||||3325008||||||||||||A'
|| chr(10)
|| 'PD1|||M|374943^Peter^DA^^^DR||11'
|| chr(10) || 'NK1|1|Chang^Micky|M||||N'
|| chr(10) || 'PV1||I|T11S^B11S^T11S-32|2~I|||P468233^PETRO^C^^^DR|374943^Peter^DA^^^DR~383675^BAWA^M^C^^MR||42002||||19||||||||PLS-1|||||||||||||||||NU||2|||201102220011
PV2||W|Stomach ache||||||||||8
OBX|1|ST|^^^ABC^Assign Benefits^PLW-HL7|||||||||||20110222
OBX|2|ST|^^^CLCGF^Client Changeable Flags^PLW-HL7||~1
OBX|3|ST|^^^LRRF^Reg Required Flags^PLW-HL7||~~~~11~1
OBX|4|ST|^^^DALOS^Discharge Authorized Length of Stay^PLW-HL7||1|d
ZUK|Q05|5K7|1|||||||N30U|T|21||||||||||||||||F83006^2.16.840.1.113883.2.1.4.3|G8515627^2.16.840.1.113883.2.1.4.2|U451777^2.16.840.1.113883.2.1.3.2.4.11||42002|||||||||A|
',
 now() --TIMESTAMP '2018-09-26 15:56:19'
        );


----- Add a second person to check we read the data back OK.
INSERT INTO TBL_IDS_MASTER (
        PatientName,
        PatientMiddleName,
        PatientSurname,
        DateOfBirth,
        NHSNumber,
        HospitalNumber,
        PatientClass,
        PatientLocation,
        AdmissionDate,
        DischargeDate,
        MessageType,
        SenderApplication,
        MessageIdentifier,
        MessageFormat,
        MessageVersion,
        MessageDateTime,
        HL7Message,
        PersistDateTime)
        VALUES(
        'Ermentrude',
        '',
        'Winfield',
        '1970-01-30', --'19700130',
        '',
        '94006100', -- NB the id below is different
        'I',
        'T11S^B11S^T11S-32',
        '2009-02-11 00:22:00', --200902110022,
        null, --'',
        'ADT^A01',
        'ADTOUT',
        'PLW21216519415491111',
        'HL7',
        '2.2',
        '2006-02-15 23:59:23', ---'20160215235923',
'MSH|^~\&|ADTOUT|PLW|||200902110022||ADT^A01|PLW21216519415491111|P|2.2|||AL|NE'
|| chr(10)
|| 'EVN|P01|200902110022||REFAD|U451777^DELL^RIMA'
|| chr(10)
|| 'PID||9876543210|94006100^^^MRENTR^MEDREC||Winfield^Ermentrude^^^MRS||19700130|F|Chang|Z1|35 Gilbert STREET^^LONDON^^KK2 9LU||020 7355 7255|Not Recorded||||3325008||||||||||||A'
|| chr(10)
|| 'PD1|||M|374943^Peter^DA^^^DR||11'
|| chr(10) || 'NK1|1|Chang^Micky|M||||N'
|| chr(10) || 'PV1||I|T11S^B11S^T11S-32|2~I|||P468233^PETRO^C^^^DR|374943^Peter^DA^^^DR~383675^BAWA^M^C^^MR||42002||||19||||||||PLS-1|||||||||||||||||NU||2|||201102220011
PV2||W|Stomach ache||||||||||8
OBX|1|ST|^^^ABC^Assign Benefits^PLW-HL7|||||||||||20110222
OBX|2|ST|^^^CLCGF^Client Changeable Flags^PLW-HL7||~1
OBX|3|ST|^^^LRRF^Reg Required Flags^PLW-HL7||~~~~11~1
OBX|4|ST|^^^DALOS^Discharge Authorized Length of Stay^PLW-HL7||1|d
ZUK|Q05|5K7|1|||||||N30U|T|21||||||||||||||||F83006^2.16.840.1.113883.2.1.4.3|G8515627^2.16.840.1.113883.2.1.4.2|U451777^2.16.840.1.113883.2.1.3.2.4.11||42002|||||||||A|
',
 now() --TIMESTAMP '2018-09-26 15:56:19'
        );
