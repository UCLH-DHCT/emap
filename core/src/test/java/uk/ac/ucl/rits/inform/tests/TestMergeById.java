package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonMrnRepository;
import uk.ac.ucl.rits.inform.informdb.Mrn;
import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonMrn;

/**
 * Set up two patients then merge them together.
 *
 * @author Jeremy Stein
 */
public class TestMergeById extends Hl7StreamEndToEndTestCase {
    @Autowired
    private PersonMrnRepository personMrnRepo;
    public TestMergeById() {
        super();
        hl7StreamFileNames.add("GenericAdt/A01.txt");
        hl7StreamFileNames.add("GenericAdt/A01_b.txt");
        hl7StreamFileNames.add("GenericAdt/A40.txt");
    }

    @Test
    @Transactional
    public void testMergeHasHappened() {
        Mrn survivingMrn = mrnRepo.findByMrnString("40800001");

        // Surviving MRN should be simpler - test this first.
        // the surviving MRN should have a single, valid link to the person, which is marked as live
        Map<Boolean, List<PersonMrn>> survivingMrnPersons = survivingMrn.getPersons().stream()
                .collect(Collectors.partitioningBy(p -> p.getValidUntil() == null));
        assertEquals(0, survivingMrnPersons.get(false).size());
        List<PersonMrn> validSurvivingMrnPersons = survivingMrnPersons.get(true);
        assertEquals(1, validSurvivingMrnPersons.size());
        // check the surviving MRN is marked as live
        PersonMrn validSurvivingMrnPerson = validSurvivingMrnPersons.get(0);
        assertTrue(validSurvivingMrnPerson.isLive());
        Person personForSurvivingMrn = validSurvivingMrnPerson.getPerson();

        // Retired MRN
        Mrn retiredMrn = mrnRepo.findByMrnString("40800000");
        Map<Boolean, List<PersonMrn>> retiredMrnPersons = retiredMrn.getPersons().stream()
                .collect(Collectors.partitioningBy(p -> p.isValid()));
        // still valid person-mrn links
        List<PersonMrn> validRetiredMrnPersons = retiredMrnPersons.get(true);

        assertEquals(1, validRetiredMrnPersons.size(), "retired mrn should still have a valid connection to a person");

        List<PersonMrn> invalidRetiredMrnPersons = retiredMrnPersons.get(false);
        assertEquals(2, invalidRetiredMrnPersons.size());
        // The retired MRN should have an invalidated link to the old person that is still marked live.
        // But its new, valid link pointing to the new person should be marked as not live.
        PersonMrn invalidRetiredMrnPerson = invalidRetiredMrnPersons.get(0);
        assertTrue(invalidRetiredMrnPerson.isLive());
        PersonMrn validRetiredMrnPerson = validRetiredMrnPersons.get(0);
        Person personForRetiredMrn = validRetiredMrnPerson.getPerson();
        assertFalse(validRetiredMrnPerson.isLive());


        // the two MRNs should point to the same person
        assertEquals(personForRetiredMrn, personForSurvivingMrn);
    }
}
