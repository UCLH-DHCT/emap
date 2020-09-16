package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnToLiveRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonData;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SpringJUnitConfig
@SpringBootTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class MessageProcessingBase {
    @Autowired
    protected MrnRepository mrnRepo;
    @Autowired
    protected MrnToLiveRepository mrnToLiveRepo;
    @Autowired
    protected CoreDemographicRepository coreDemographicRepository;
    @Autowired
    protected PersonData personData;

    @Autowired
    protected InformDbOperations dbOps;

    protected final String defaultMrn = "40800000";


    protected final InterchangeMessageFactory messageFactory = new InterchangeMessageFactory();


    @Transactional
    protected void processSingleMessage(EmapOperationMessage msg) throws EmapOperationMessageProcessingException {
        try {
            msg.processMessage(dbOps);
        } catch (MessageIgnoredException e) {
            if (!false) {
                throw e;
            }
        }
    }

    protected List<Mrn> getAllMrns() {
        return StreamSupport.stream(mrnRepo.findAll().spliterator(), false).collect(Collectors.toList());
    }

}
