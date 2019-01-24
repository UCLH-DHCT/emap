-- psql -f insert_record_to_dummy_ids.cmd DUMMY_IDS
--
----- Add a record to IDS.
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
        'Leonardo',
        'Chuck',
        'Da Vinci',
        '1500-01-01',---'15000101',
        '',
        '94006200', -- NB the id below is different
        'I',
        'ICU-16',
        '2009-02-11 00:22:00', ---200902110022,
        null,
        'ADT^A02',
        'ADTOUT',
        'PLW21216519415491111',
        'HL7',
        '2.2',
        now(), ----'20181015111346',
'MSH|^~\&|ADTOUT|PLW|||200902110022||ADT^A01|PLW21216519415491111|P|2.2|||AL|NE'
|| chr(13)
|| 'EVN|P01|200902110022||REFAD|U451777^DELL^RIMA'
|| chr(13)
|| 'PID||9876543210|94006200^^^MRENTR^MEDREC||Winfield^Ermentrude^^^MRS||19700130|F|Chang|Z1|35 Gilbert STREET^^LONDON^^KK2 9LU||020 7355 7255|Not Recorded||||3325008||||||||||||A'
|| chr(13)
|| 'PD1|||M|374943^Peter^DA^^^DR||11'
|| chr(13) || 'NK1|1|Chang^Micky|M||||N'
|| chr(13) || 'PV1||I|T11S^B11S^T11S-32|2~I|||P468233^PETRO^C^^^DR|374943^Peter^DA^^^DR~383675^BAWA^M^C^^MR||42002||||19||||||||PLS-1|||||||||||||||||NU||2|||201102220011
PV2||W|Stomach ache||||||||||8
OBX|1|ST|^^^ABC^Assign Benefits^PLW-HL7|||||||||||20110222
OBX|2|ST|^^^CLCGF^Client Changeable Flags^PLW-HL7||~1
OBX|3|ST|^^^LRRF^Reg Required Flags^PLW-HL7||~~~~11~1
OBX|4|ST|^^^DALOS^Discharge Authorized Length of Stay^PLW-HL7||1|d
ZUK|Q05|5K7|1|||||||N30U|T|21||||||||||||||||F83006^2.16.840.1.113883.2.1.4.3|G8515627^2.16.840.1.113883.2.1.4.2|U451777^2.16.840.1.113883.2.1.3.2.4.11||42002|||||||||A|
',
 now() --TIMESTAMP '2018-09-26 15:56:19'
        );
