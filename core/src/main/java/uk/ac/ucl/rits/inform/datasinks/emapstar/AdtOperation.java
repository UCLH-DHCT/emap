package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.time.Instant;

/**
 * Implement ADT for the Emap-Star DB v2.
 * @author Stef Piatek
 */
@Component
public class AdtOperation {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonRepository personRepo;

    public AdtOperation(PersonRepository personRepo) {
        this.personRepo = personRepo;
    }


    public String processMessage(AdtMessage msg, Instant storedFrom) {
        String returnCode = "OK";
        String sourceSystem = "EPIC";
        Mrn mrn = personRepo.getOrCreateMrn(msg.getMrn(), msg.getNhsNumber(), sourceSystem, msg.getEventOccurredDateTime(), storedFrom);
        return returnCode;
    }
}
