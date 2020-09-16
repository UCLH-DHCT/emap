package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.MergeById;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestAdtProcessing extends MessageProcessingBase {

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
        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdEquals(mrns.get(0).getMrnId());
        assertTrue(demographic.isPresent());
        assertEquals("ORANGE", demographic.get().getLastname());
        assertTrue(demographic.get().isAlive());
        assertNotNull(demographic.get().getDatetimeOfBirth());
    }

    /**
     * Mrn already exists
     * no new Mrns should be created but demographics should be updated with known data from the message.
     */
    @Test
    @Sql(value = "/populate_mrn.sql")
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
        CoreDemographic demographic =  coreDemographicRepository.getByMrnIdEquals(mrn.getMrnId()).orElseThrow(NullPointerException::new);
        assertEquals("middle", demographic.getMiddlename()); // unknown value so shouldn't change
        assertEquals("ORANGE", demographic.getLastname());  // known value so should change
    }

    /**
     * Message is older than current information, so demographics should stay the same
     */
    @Test
    @Sql(value = "/populate_mrn.sql")
    public void testOldDemographicsData() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        msg.setRecordedDateTime(Instant.MIN);

        Mrn mrn = mrnRepo.getByMrnEquals(defaultMrn);
        CoreDemographic preDemographic = coreDemographicRepository.getByMrnIdEquals(mrn.getMrnId()).orElseThrow(NullPointerException::new);

        // process message
        dbOps.processMessage(msg);

        CoreDemographic postDemographic = coreDemographicRepository.getByMrnIdEquals(mrn.getMrnId()).orElseThrow(NullPointerException::new);
        assertEquals(preDemographic, postDemographic);
    }

    /**
     * Mrn (id=2) already exists and has been merged (live id=3)
     * No new mrns should be created, processing should be done on the live id only and demographics should be udated
     */
    @Test
    @Sql(value = "/populate_mrn.sql")
    public void testMrnExistsAndIsntLive() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        String mrnString = "60600000";
        msg.setMrn(new Hl7Value<>(mrnString));

        int startingMrnCount = getAllMrns().size();

        // process message
        dbOps.processMessage(msg);
        Mrn mrn = mrnRepo.getByMrnEquals(mrnString);
        // no new mrns added, existing id is kept
        assertEquals(startingMrnCount, getAllMrns().size());
        assertEquals(1002L, mrn.getMrnId().longValue());

        long liveMrnId = 1003L;
        //person repo should return the live mrn only
        Mrn liveMrn = personData.getOrCreateMrn(msg.getMrn().get(), msg.getNhsNumber().get(), null, null, null);
        assertEquals(liveMrnId, liveMrn.getMrnId().longValue());

        // demographics that are updated should be the live mrn
        Optional<CoreDemographic> demographic = coreDemographicRepository.getByMrnIdEquals(liveMrnId);
        assertTrue(demographic.isPresent());
        assertEquals("ORANGE", demographic.get().getLastname());
    }

    /**
     * retire existing mrn, merge into new mrn
     * should change the mrnToLive for retired MRN to surviving Mrn and create a new
     */
    @Test
    @Sql(value = "/populate_mrn.sql")
    public void testMergeKnownRetiringNewSurviving() throws EmapOperationMessageProcessingException {
        MergeById msg = messageFactory.getAdtMessage("generic/A40.yaml");

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
    @Sql(value = "/populate_mrn.sql")
    public void testMergeNewRetiringAlreadyMergedSurviving() throws EmapOperationMessageProcessingException {
        String messageSurvivingMrn = "60600000";
        String retiringMrnString = "60600005";
        MergeById msg = messageFactory.getAdtMessage("generic/A40.yaml");
        msg.setRetiredMrn(retiringMrnString);
        msg.setMrn(new Hl7Value<>(messageSurvivingMrn));

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

}
