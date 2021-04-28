package uk.ac.ucl.rits.inform.datasinks.emapstar.adt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnToLiveAuditRepository;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographicAudit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLiveAudit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestAdtProcessingPerson extends MessageProcessingBase {
    @Autowired
    private CoreDemographicRepository coreDemographicRepository;

    @Autowired
    private CoreDemographicAuditRepository coreDemographicAuditRepository;

    @Autowired
    private MrnToLiveAuditRepository mrnToLiveAuditRepository;

    private List<CoreDemographicAudit> getAllAuditCoreDemographics() {
        return StreamSupport.stream(coreDemographicAuditRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    private String newMrnString = "60600000";


    /**
     * no existing mrns, so new mrn, mrn_to_live core_demographics rows should be created.
     */
    @Test
    void testCreateNewPatient() throws Exception {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        dbOps.processMessage(msg);
        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);
        CoreDemographic demographic = coreDemographicRepository.getByMrnIdEquals(mrns.get(0)).orElseThrow();
        assertEquals("ORANGE", demographic.getLastname());
        assertTrue(demographic.getAlive());
        assertNotNull(demographic.getDatetimeOfBirth());
        assertEquals("Refused to Give", demographic.getEthnicity());
    }

    /**
     * move visit information when neither exists in the database.
     * Should create previous and current MRN, and the core demographics for the current MRN.
     */
    @Test
    void testMoveVisitInformationCreatesMrnsIfTheyDontExist() throws Exception {
        MoveVisitInformation msg = messageFactory.getAdtMessage("generic/A45.yaml");
        dbOps.processMessage(msg);
        List<Mrn> mrns = getAllMrns();
        assertEquals(2, mrns.size());

        Mrn newMrn = mrnRepo.getByMrnEquals(newMrnString).orElseThrow();

        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdEquals(newMrn);
        assertTrue(demographic.isPresent());
    }

    /**
     * no MRNs exist in database, so a new MRN should be created with the correct final MRN.
     * @throws Exception shouldn't happen
     */
    @Test
    void testChangePatientIdentifiersCreatesNewMrn() throws Exception {
        ChangePatientIdentifiers msg = messageFactory.getAdtMessage("generic/A47.yaml");

        //process message
        dbOps.processMessage(msg);
        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());

        assertEquals("40800001", mrns.get(0).getMrn());
    }


    /**
     * Mrn already exists
     * no new Mrns should be created but demographics should be updated with known data from the message.
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testMrnExists() throws Exception {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        int startingMrnCount = getAllMrns().size();
        // process message
        dbOps.processMessage(msg);
        Mrn mrn = mrnRepo.getByMrnEquals(defaultMrn).orElseThrow();
        // no new mrns added, existing id is kept
        assertEquals(startingMrnCount, getAllMrns().size());
        assertEquals(1001L, mrn.getMrnId().longValue());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrn);
        assertEquals(1001L, mrnToLive.getLiveMrnId().getMrnId().longValue());

        // unknown demographics should not be set
        CoreDemographic demographic = coreDemographicRepository.getByMrnIdEquals(mrn).orElseThrow(NullPointerException::new);
        assertEquals("middle", demographic.getMiddlename()); // unknown value so shouldn't change
        assertEquals("ORANGE", demographic.getLastname());  // known value so should change
    }

    /**
     * Message is older than current information, so demographics should stay the same and no audit log rows should be added.
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testOldAdtMessage() throws Exception {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        msg.setEventOccurredDateTime(Instant.parse("2010-01-01T01:01:01Z"));

        Mrn mrn = mrnRepo.getByMrnEquals(defaultMrn).orElseThrow();
        CoreDemographic preDemographic = coreDemographicRepository.getByMrnIdEquals(mrn).orElseThrow(NullPointerException::new);

        // process message
        dbOps.processMessage(msg);

        CoreDemographic postDemographic = coreDemographicRepository.getByMrnIdEquals(mrn).orElseThrow(NullPointerException::new);
        assertEquals(preDemographic.getCoreDemographicId(), postDemographic.getCoreDemographicId());

        List<CoreDemographicAudit> audit = getAllAuditCoreDemographics();
        assertTrue(audit.isEmpty());

        // audit mrn to live should not be added to
        List<MrnToLiveAudit> mrnToLiveAudit = StreamSupport.stream(mrnToLiveAuditRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        assertTrue(mrnToLiveAudit.isEmpty());
    }

    /**
     * Mrn (id=2) already exists and has been merged (live id=3).
     * No new mrns should be created, processing should be done on the live id only and demographics should be updated
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testMrnExistsAndIsntLive() throws Exception {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        msg = setDataForHospitalVisitId4002(msg);

        int startingMrnCount = getAllMrns().size();

        // process message
        dbOps.processMessage(msg);
        Mrn mrn = mrnRepo.getByMrnEquals(newMrnString).orElseThrow();
        // no new mrns added, existing id is kept
        assertEquals(startingMrnCount, getAllMrns().size());
        assertEquals(1002L, mrn.getMrnId().longValue());

        long liveMrnId = 1003L;
        //person repo should return the live mrn only
        Mrn liveMrn = personController.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), "EPIC", Instant.now(), Instant.now());
        assertEquals(liveMrnId, liveMrn.getMrnId().longValue());

        // demographics that are updated should be the live mrn
        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdMrnIdEquals(liveMrnId);
        assertTrue(demographic.isPresent());
        assertEquals("ORANGE", demographic.get().getLastname());
    }

    /**
     * retire existing mrn, merge into new mrn.
     * should change the mrnToLive for retired MRN to surviving Mrn and create a new
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testMergeKnownRetiringNewSurviving() throws Exception {
        MergePatient msg = messageFactory.getAdtMessage("generic/A40.yaml");
        msg.setRecordedDateTime(msg.getRecordedDateTime().plus(1, ChronoUnit.HOURS));

        // process message
        dbOps.processMessage(msg);
        MrnToLive retiredMrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrnRepo.getByMrnEquals(defaultMrn).orElseThrow());
        Mrn newMrn = mrnRepo.getByMrnEquals("40800001").orElseThrow();
        assertEquals(newMrn.getMrnId(), retiredMrnToLive.getLiveMrnId().getMrnId());
        // check number of mrn to live rows by live mrn
        List<MrnToLive> survivingMrnToLiveRows = mrnToLiveRepo.getAllByLiveMrnIdEquals(newMrn);
        assertEquals(2, survivingMrnToLiveRows.size());
    }

    /**
     * retire mrn that hasn't been seen before, merging into MRN which has already been merged.
     * should create a new mrn for the unseen mrn, then merge it directly to the final live mrn
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testMergeNewRetiringAlreadyMergedSurviving() throws Exception {
        String retiringMrnString = "60600005";
        MergePatient msg = messageFactory.getAdtMessage("generic/A40.yaml");
        msg.setPreviousMrn(retiringMrnString);
        msg = setDataForHospitalVisitId4002(msg);

        String liveMrnString = "30700000";

        // process message
        dbOps.processMessage(msg);
        // retiring mrn created and linked to surviving mrn
        Mrn retiringMrn = mrnRepo.getByMrnEquals(retiringMrnString).orElseThrow();
        assertNotNull(retiringMrn);
        MrnToLive retiredMrnToLive = mrnToLiveRepo.getByMrnIdEquals(retiringMrn);
        Mrn survivingMrn = mrnRepo.getByMrnEquals(liveMrnString).orElseThrow();
        assertEquals(survivingMrn.getMrnId(), retiredMrnToLive.getLiveMrnId().getMrnId());
        // check number of mrn to live rows by live mrn
        List<MrnToLive> survivingMrnToLiveRows = mrnToLiveRepo.getAllByLiveMrnIdEquals(survivingMrn);
        assertEquals(3, survivingMrnToLiveRows.size());
    }

    /**
     * Merging patient that is known by Mrn and Nhs number.
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testMergeByNhsNumber() throws Exception {
        String survivingMrnString = "30700000";
        String retiringMrnString = "60600000";
        String retiringNhsNumber = "222222222";
        MergePatient msg = messageFactory.getAdtMessage("generic/A40.yaml");
        msg.setPreviousMrn(retiringMrnString);
        msg.setPreviousNhsNumber(retiringNhsNumber);
        msg.setMrn(survivingMrnString);

        // process message
        dbOps.processMessage(msg);
        // retiring mrn created and linked to surviving mrn
        Mrn retiringMrn = mrnRepo.getAllByNhsNumberEquals(retiringNhsNumber).stream()
                .filter(mrn -> mrn.getMrn() == null)
                .findFirst().orElseThrow(NullPointerException::new);
        MrnToLive retiredMrnToLive = mrnToLiveRepo.getByMrnIdEquals(retiringMrn);
        Mrn survivingMrn = mrnRepo.getByMrnEquals(survivingMrnString).orElseThrow();
        assertEquals(survivingMrn.getMrnId(), retiredMrnToLive.getLiveMrnId().getMrnId());
        // check number of mrn to live rows by live mrn
        List<MrnToLive> survivingMrnToLiveRows = mrnToLiveRepo.getAllByLiveMrnIdEquals(survivingMrn);
        assertEquals(3, survivingMrnToLiveRows.size());
    }

    /**
     * Two messages for an existing MRN are both newer and different are being processed.
     * Audit log should have two entries that are the original state when updated.
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testCoreDemographicsAuditLog() throws Exception {
        // first message as MrnExists
        AdmitPatient msg1 = messageFactory.getAdtMessage("generic/A01.yaml");
        AdmitPatient msg2 = messageFactory.getAdtMessage("generic/A01.yaml");
        msg2.setEventOccurredDateTime(Instant.parse("2020-10-01T00:00:00Z"));
        msg2.setPatientMiddleName(InterchangeValue.buildFromHl7("lime"));

        // process messages
        dbOps.processMessage(msg1);
        dbOps.processMessage(msg2);

        long coreDemographicId = 3002;

        // audit log for demographics should be populated
        List<CoreDemographicAudit> audit = coreDemographicAuditRepository.getAllByCoreDemographicId(coreDemographicId);
        assertEquals(2, audit.size());


        // original state of the demographics should be saved to audit
        CoreDemographicAudit firstAudit = audit.stream()
                .min(Comparator.comparing(CoreDemographicAudit::getStoredUntil))
                .orElseThrow(NullPointerException::new);
        assertEquals("zest", firstAudit.getLastname());

        // second message should have the updates from the first message being saved in audit
        CoreDemographicAudit secondAudit = audit.stream()
                .max(Comparator.comparing(CoreDemographicAudit::getStoredUntil))
                .orElseThrow(NullPointerException::new);
        assertEquals("ORANGE", secondAudit.getLastname());
    }


    @Test
    @Sql(value = "/populate_db.sql")
    void testCoreDemographicsAuditWithDuplicateDemographics() throws Exception {
        // first message as MrnExists
        AdmitPatient msg1 = messageFactory.getAdtMessage("generic/A01.yaml");
        AdmitPatient msg2 = messageFactory.getAdtMessage("generic/A01.yaml");
        msg2.setRecordedDateTime(Instant.parse("2020-10-01T00:00:00Z"));

        // process messages
        dbOps.processMessage(msg1);
        dbOps.processMessage(msg2);

        long coreDemographicId = 3002;

        // audit log for demographics should be populated only by the first message
        List<CoreDemographicAudit> audit = coreDemographicAuditRepository.getAllByCoreDemographicId(coreDemographicId);
        assertEquals(1, audit.size());
    }

    /**
     * One merge message for MRN that is live for two rows in mrn_to_live.
     * Two rows should be added to audit log, both with the retiring Mrn as the live mrn
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testMrnToLiveAuditLog() throws Exception {
        // exists and has two entities mapped
        String retiringMrnString = "30700000";
        String messageSurvivingMrn = "44444444";
        MergePatient msg = messageFactory.getAdtMessage("generic/A40.yaml");
        msg.setPreviousMrn(retiringMrnString);
        msg.setMrn(messageSurvivingMrn);

        // process message
        dbOps.processMessage(msg);

        // audit log for demographics should be populated
        List<MrnToLiveAudit> audits = mrnToLiveAuditRepository.getAllByLiveMrnIdMrn(retiringMrnString);
        assertEquals(2, audits.size());

        // original live should be saved to audit
        for (MrnToLiveAudit audit : audits) {
            assertEquals(retiringMrnString, audit.getLiveMrnId().getMrn());
        }
    }

    /**
     * Delete person information should delete the core demographics and log a row in the audit table.
     * @throws Exception shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testDeletePersonInformation() throws Exception {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        // process message
        dbOps.processMessage(msg);

        Mrn mrn = mrnRepo.getByMrnEquals(defaultMrn).orElseThrow();
        // no demographics should exist
        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdEquals(mrn);
        assertFalse(demographic.isPresent());
        // audit should have one row for deleted demographics
        List<CoreDemographicAudit> audits = coreDemographicAuditRepository.getAllByMrnIdMrn(defaultMrn);
        assertEquals(1, audits.size());
    }

    /**
     * Message is older than database, so no deletes should take place.
     * @throws Exception shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testOldDeleteMessageHasNoEffect() throws Exception {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        msg.setEventOccurredDateTime(Instant.parse("2000-01-01T00:00:00Z"));
        // process message
        dbOps.processMessage(msg);

        Mrn mrn = mrnRepo.getByMrnEquals(defaultMrn).orElseThrow();
        // should still exist
        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdEquals(mrn);
        assertTrue(demographic.isPresent());
        // no audit row
        List<CoreDemographicAudit> audits = coreDemographicAuditRepository.getAllByMrnIdMrn(defaultMrn);
        assertEquals(0, audits.size());
    }

    /**
     * Change patient identifiers, new identifier doesn't already exist so the mrn should be changed.
     * @throws Exception shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testChangePatientIdentifiers() throws Exception {
        ChangePatientIdentifiers msg = messageFactory.getAdtMessage("generic/A47.yaml");

        // save state before processing to be sure that it works
        Optional<Mrn> previousMrnBeforeProcessing = mrnRepo.findByMrnEquals(defaultMrn);
        Optional<Mrn> newMrnBeforeProcessing = mrnRepo.findByMrnEquals("40800001");

        //process message
        dbOps.processMessage(msg);

        // previous Mrn should go from existing previously, to now not existing
        Optional<Mrn> previousMrn = mrnRepo.findByMrnEquals(defaultMrn);
        assertTrue(previousMrnBeforeProcessing.isPresent());
        assertFalse(previousMrn.isPresent());

        // new Mrn should go from not existing previously, to now existing
        Optional<Mrn> newMrn = mrnRepo.findByMrnEquals("40800001");
        assertFalse(newMrnBeforeProcessing.isPresent());
        assertTrue(newMrn.isPresent());
    }

    /**
     * Change patient identifiers, final MRN already exists, so should throw an exception.
     * @throws Exception shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    void testChangePatientIdentifiersWithExistingFinalMrn() throws Exception {
        ChangePatientIdentifiers msg = messageFactory.getAdtMessage("generic/A47.yaml");
        msg.setMrn(newMrnString);

        assertThrows(IncompatibleDatabaseStateException.class, () -> dbOps.processMessage(msg));
    }

    private void createAndProcess(String nhsNumber, String Mrn, Integer minuteAdded) throws Exception {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");

        msg.setNhsNumber(nhsNumber);
        msg.setMrn(Mrn);
        msg.setVisitNumber(minuteAdded.toString());
        msg.setEventOccurredDateTime(past.plus(minuteAdded, ChronoUnit.MINUTES));
        dbOps.processMessage(msg);
    }

    /**
     * Test that combinations of identifiers for the same patient are processed without error.
     * @throws Exception shouldn't happen
     */
    @Test
    void testNhsNumberUpdates() throws Exception {
        // first mrn
        createAndProcess(null, "mrn", 0);
        createAndProcess("nhs", "mrn", 2);

        // second mrn
        createAndProcess("nhs2", null, 1);
        createAndProcess("nhs2", "mrn2", 3);
        createAndProcess("nhs3", "mrn2", 4);


        // first MRN should only exist once in the database and have NHS number added to it
        Mrn firstMrn = mrnRepo.findByMrnEquals("mrn").orElseThrow();
        assertNotNull(firstMrn.getNhsNumber());

        // second MRN should now be updated with the new NHS number
        Mrn secondMrn = mrnRepo.findByMrnEquals("mrn2").orElseThrow();
        assertEquals("nhs3", secondMrn.getNhsNumber());

        // should have 2 MRNs
        List<Mrn> mrns = getAllMrns();
        assertEquals(2, mrns.size());
    }

}
