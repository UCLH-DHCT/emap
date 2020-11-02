package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnToLiveAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicRepository;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographicAudit;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLiveAudit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestAdtProcessingPerson extends MessageProcessingBase {
    @Autowired
    CoreDemographicRepository coreDemographicRepository;

    @Autowired
    CoreDemographicAuditRepository coreDemographicAuditRepository;

    @Autowired
    MrnToLiveAuditRepository mrnToLiveAuditRepository;

    private List<CoreDemographicAudit> getAllAuditCoreDemographics() {
        return StreamSupport.stream(coreDemographicAuditRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    String newMrnString = "60600000";


    /**
     * no existing mrns, so new mrn, mrn_to_live core_demographics rows should be created
     */
    @Test
    public void testCreateNewPatient() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        dbOps.processMessage(msg);
        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);
        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdEquals(mrns.get(0));
        assertTrue(demographic.isPresent());
        assertEquals("ORANGE", demographic.get().getLastname());
        assertTrue(demographic.get().isAlive());
        assertNotNull(demographic.get().getDatetimeOfBirth());
    }

    /**
     * move visit information when neither exists in the database.
     * Should create previous and current MRN, and the core demographics for the current MRN.
     */
    @Test
    public void testMoveVisitInformationCreatesMrnsIfTheyDontExist() throws EmapOperationMessageProcessingException {
        MoveVisitInformation msg = messageFactory.getAdtMessage("generic/A45.yaml");
        dbOps.processMessage(msg);
        List<Mrn> mrns = getAllMrns();
        assertEquals(2, mrns.size());

        Mrn newMrn = mrnRepo.getByMrnEquals(newMrnString);

        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdEquals(newMrn);
        assertTrue(demographic.isPresent());
    }

    /**
     * no MRNs exist in database, so a new MRN should be created with the correct final MRN
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    public void testChangePatientIdentifiersCreatesNewMrn() throws EmapOperationMessageProcessingException {
        ChangePatientIdentifiers msg = messageFactory.getAdtMessage("generic/A47.yaml");

        //process message
        dbOps.processMessage(msg);
        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());

        assertEquals( "40800001", mrns.get(0).getMrn());
    }


        /**
         * Mrn already exists
         * no new Mrns should be created but demographics should be updated with known data from the message.
         */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testMrnExists() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        int startingMrnCount = getAllMrns().size();
        // process message
        dbOps.processMessage(msg);
        Mrn mrn = mrnRepo.getByMrnEquals(defaultMrn);
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
     * Message is older than current information, so demographics should stay the same and no audit log rows should be added
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testOldAdtMessage() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        msg.setEventOccurredDateTime(Instant.parse("2010-01-01T01:01:01Z"));

        Mrn mrn = mrnRepo.getByMrnEquals(defaultMrn);
        CoreDemographic preDemographic = coreDemographicRepository.getByMrnIdEquals(mrn).orElseThrow(NullPointerException::new);

        // process message
        dbOps.processMessage(msg);

        CoreDemographic postDemographic = coreDemographicRepository.getByMrnIdEquals(mrn).orElseThrow(NullPointerException::new);
        assertEquals(preDemographic, postDemographic);

        List<CoreDemographicAudit> audit = getAllAuditCoreDemographics();
        assertTrue(audit.isEmpty());

        // audit mrn to live should not be added to
        List<MrnToLiveAudit> mrnToLiveAudit = StreamSupport.stream(mrnToLiveAuditRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        assertTrue(mrnToLiveAudit.isEmpty());
    }

    /**
     * Mrn (id=2) already exists and has been merged (live id=3)
     * No new mrns should be created, processing should be done on the live id only and demographics should be updated
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testMrnExistsAndIsntLive() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        msg.setMrn(newMrnString);

        int startingMrnCount = getAllMrns().size();

        // process message
        dbOps.processMessage(msg);
        Mrn mrn = mrnRepo.getByMrnEquals(newMrnString);
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
     * retire existing mrn, merge into new mrn
     * should change the mrnToLive for retired MRN to surviving Mrn and create a new
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testMergeKnownRetiringNewSurviving() throws EmapOperationMessageProcessingException {
        MergePatient msg = messageFactory.getAdtMessage("generic/A40.yaml");
        msg.setRecordedDateTime(msg.getRecordedDateTime().plus(1, ChronoUnit.HOURS));

        // process message
        dbOps.processMessage(msg);
        MrnToLive retiredMrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrnRepo.getByMrnEquals(defaultMrn));
        Mrn newMrn = mrnRepo.getByMrnEquals("40800001");
        assertEquals(newMrn, retiredMrnToLive.getLiveMrnId());
        // check number of mrn to live rows by live mrn
        List<MrnToLive> survivingMrnToLiveRows = mrnToLiveRepo.getAllByLiveMrnIdEquals(newMrn);
        assertEquals(2, survivingMrnToLiveRows.size());
    }

    /**
     * retire mrn that hasn't been seen before, merging into MRN which has already been merged
     * should create a new mrn for the unseen mrn, then merge it directly to the final live mrn
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testMergeNewRetiringAlreadyMergedSurviving() throws EmapOperationMessageProcessingException {
        String messageSurvivingMrn = "60600000";
        String retiringMrnString = "60600005";
        MergePatient msg = messageFactory.getAdtMessage("generic/A40.yaml");
        msg.setPreviousMrn(retiringMrnString);
        msg.setMrn(messageSurvivingMrn);

        String liveMrnString = "30700000";

        // process message
        dbOps.processMessage(msg);
        // retiring mrn created and linked to surviving mrn
        Mrn retiringMrn = mrnRepo.getByMrnEquals(retiringMrnString);
        assertNotNull(retiringMrn);
        MrnToLive retiredMrnToLive = mrnToLiveRepo.getByMrnIdEquals(retiringMrn);
        Mrn survivingMrn = mrnRepo.getByMrnEquals(liveMrnString);
        assertEquals(survivingMrn, retiredMrnToLive.getLiveMrnId());
        // check number of mrn to live rows by live mrn
        List<MrnToLive> survivingMrnToLiveRows = mrnToLiveRepo.getAllByLiveMrnIdEquals(survivingMrn);
        assertEquals(3, survivingMrnToLiveRows.size());
    }

    /**
     * Merging patient that is known by Mrn and Nhs number
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testMergeByNhsNumber() throws EmapOperationMessageProcessingException {
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
        Mrn survivingMrn = mrnRepo.getByMrnEquals(survivingMrnString);
        assertEquals(survivingMrn, retiredMrnToLive.getLiveMrnId());
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
    public void testCoreDemographicsAuditLog() throws EmapOperationMessageProcessingException {
        // first message as MrnExists
        AdmitPatient msg1 = messageFactory.getAdtMessage("generic/A01.yaml");
        AdmitPatient msg2 = messageFactory.getAdtMessage("generic/A01.yaml");
        msg2.setEventOccurredDateTime(Instant.parse("2020-10-01T00:00:00Z"));
        msg2.setPatientMiddleName(Hl7Value.buildFromHl7("lime"));

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
    public void testCoreDemographicsAuditWithDuplicateDemographics() throws EmapOperationMessageProcessingException {
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
    public void testMrnToLiveAuditLog() throws EmapOperationMessageProcessingException {
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
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testDeletePersonInformation() throws EmapOperationMessageProcessingException {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        // process message
        dbOps.processMessage(msg);

        Mrn mrn = mrnRepo.getByMrnEquals(defaultMrn);
        // no demographics should exist
        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdEquals(mrn);
        assertFalse(demographic.isPresent());
        // audit should have one row for deleted demographics
        List<CoreDemographicAudit> audits = coreDemographicAuditRepository.getAllByMrnIdMrn(defaultMrn);
        assertEquals(1, audits.size());
    }

    /**
     * Message is older than database, so no deletes should take place.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testOldDeleteMessageHasNoEffect() throws EmapOperationMessageProcessingException {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        msg.setEventOccurredDateTime(Instant.parse("2000-01-01T00:00:00Z"));
        // process message
        dbOps.processMessage(msg);

        Mrn mrn = mrnRepo.getByMrnEquals(defaultMrn);
        // should still exist
        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdEquals(mrn);
        assertTrue(demographic.isPresent());
        // no audit row
        List<CoreDemographicAudit> audits = coreDemographicAuditRepository.getAllByMrnIdMrn(defaultMrn);
        assertEquals(0, audits.size());
    }

    /**
     * Change patient identifiers, new identifier doesn't already exist so the mrn should be changed
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testChangePatientIdentifiers() throws EmapOperationMessageProcessingException {
        ChangePatientIdentifiers msg = messageFactory.getAdtMessage("generic/A47.yaml");

        // save state before processing to be sure that it works
        Optional<Object> previousMrnBeforeProcessing = mrnRepo.getAllByMrnEquals(defaultMrn);
        Optional<Object> newMrnBeforeProcessing = mrnRepo.getAllByMrnEquals("40800001");

        //process message
        dbOps.processMessage(msg);

        // previous Mrn should go from existing previously, to now not existing
        Optional<Object> previousMrn = mrnRepo.getAllByMrnEquals(defaultMrn);
        assertTrue(previousMrnBeforeProcessing.isPresent());
        assertFalse(previousMrn.isPresent());

        // new Mrn should go from not existing previously, to now existing
        Optional<Object> newMrn = mrnRepo.getAllByMrnEquals("40800001");
        assertFalse(newMrnBeforeProcessing.isPresent());
        assertTrue(newMrn.isPresent());
    }

    /**
     * Change patient identifiers, final MRN already exists, so should throw an exception
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql(value = "/populate_db.sql")
    public void testChangePatientIdentifiersWithExistingFinalMrn() throws EmapOperationMessageProcessingException {
        ChangePatientIdentifiers msg = messageFactory.getAdtMessage("generic/A47.yaml");
        msg.setMrn(newMrnString);

        assertThrows(IllegalArgumentException.class, () -> dbOps.processMessage(msg));
    }

}
