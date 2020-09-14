package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnToLiveRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.MergeById;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SpringJUnitConfig
@SpringBootTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestMrn {
    @Autowired
    private MrnRepository mrnRepo;
    @Autowired
    private MrnToLiveRepository mrnToLiveRepo;
    @Autowired
    private PersonRepository personRepo;

    @Autowired
    private InformDbOperations dbOps;

    private final String defaultMrn = "40800000";


    InterchangeMessageFactory messageFactory = new InterchangeMessageFactory();


    @Transactional
    protected void processSingleMessage(boolean allowMessageIgnored, EmapOperationMessage msg) throws EmapOperationMessageProcessingException {
        try {
            msg.processMessage(dbOps);
        } catch (MessageIgnoredException e) {
            if (!allowMessageIgnored) {
                throw e;
            }
        }
    }

    private List<Mrn> getAllMrns() {
        return StreamSupport.stream(mrnRepo.findAll().spliterator(), false).collect(Collectors.toList());
    }


    /**
     * no existing mrns, so new mrn and mrn_to_live should be created
     */
    @Test
    public void testCreateNewMrn() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        dbOps.processMessage(msg);
        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);
    }

    /**
     * Mrn already exists so no new Mrns should be created
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
    }

    /**
     * Mrn (id=2) already exists and has been merged (live id=3)
     * No new mrns should be created, processing should be done on the live id only
     */
    @Test
    @Sql(value = "/populate_mrn.sql")
    public void testMrnExistsAndWasMerged() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        String mrnString = "60600000";
        msg.setMrn(mrnString);

        int startingMrnCount = getAllMrns().size();

        // process message
        dbOps.processMessage(msg);
        Mrn mrn = mrnRepo.getByMrnEquals(mrnString);
        // no new mrns added, existing id is kept
        assertEquals(startingMrnCount, getAllMrns().size());
        assertEquals(1002L, mrn.getMrnId().longValue());

        //person repo should return the live mrn only
        Mrn liveMrn = personRepo.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), null, null, null);
        assertEquals(1003L, liveMrn.getMrnId().longValue());
        // TODO: when demographics are added, check that the demographics get added to the live MRN only
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
        assertEquals(2,  survivingMrnToLiveRows.size());
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
        assertEquals(3,  survivingMrnToLiveRows.size());
    }

}
