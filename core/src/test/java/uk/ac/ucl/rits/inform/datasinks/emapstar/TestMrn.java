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
    MrnRepository mrnRepo;
    @Autowired
    MrnToLiveRepository mrnToLiveRepo;
    @Autowired
    PersonRepository personRepo;

    @Autowired
    InformDbOperations dbOps;


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
        Mrn mrn = mrnRepo.getByMrnEqualsOrMrnIsNullAndNhsNumberEquals("40800000", null).get();
        // no new mrns added, existing id is kept
        assertEquals(startingMrnCount, getAllMrns().size());
        assertEquals(1L, mrn.getMrnId().longValue());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrn);
        assertEquals(1L, mrnToLive.getLiveMrnId().getMrnId().longValue());
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
        Mrn mrn = mrnRepo.getByMrnEqualsOrMrnIsNullAndNhsNumberEquals(mrnString, null).get();
        // no new mrns added, existing id is kept
        assertEquals(startingMrnCount, getAllMrns().size());
        assertEquals(2L, mrn.getMrnId().longValue());

        //person repo should return the live mrn only
        Mrn liveMrn = personRepo.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), null, null, null);
        assertEquals(3L, liveMrn.getMrnId().longValue());
        // TODO: when demographics are added, check that the demographics get added to the live MRN only
    }

}
