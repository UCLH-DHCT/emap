package uk.ac.ucl.rits.inform.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonMrnRepository;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.Mrn;
import uk.ac.ucl.rits.inform.informdb.MrnEncounter;
import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonMrn;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { uk.ac.ucl.rits.inform.datasinks.emapstar.App.class })
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TestMergeById extends Hl7StreamTestCase {
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
        Mrn retiredMrn = mrnRepo.findByMrnString("40800000");
        Mrn survivingMrn = mrnRepo.findByMrnString("40800001");
        Map<Boolean, List<PersonMrn>> retiredMrnPersons = retiredMrn.getPersons().stream()
                .collect(Collectors.partitioningBy(p -> p.getValidUntil() == null));
        // still valid person-mrn links
        List<PersonMrn> validRetiredMrnPersons = retiredMrnPersons.get(true);

        // this fails but shouldn't
        assertEquals("retired mrn should still have a valid connection to a person", 1, validRetiredMrnPersons.size());

        List<PersonMrn> invalidRetiredMrnPersons = retiredMrnPersons.get(false);
        assertEquals(1, invalidRetiredMrnPersons.size());
        // check the retired MRN is no longer marked live
        PersonMrn invalidRetiredMrnPerson = invalidRetiredMrnPersons.get(0);
        //assertFalse(invalidRetiredMrnPerson.isLive());
        Person personForRetiredMrn = validRetiredMrnPersons.get(0).getPerson();

        Map<Boolean, List<PersonMrn>> survivingMrnPersons = survivingMrn.getPersons().stream()
                .collect(Collectors.partitioningBy(p -> p.getValidUntil() == null));
        assertEquals(0, survivingMrnPersons.get(false).size());
        List<PersonMrn> validSurvivingMrnPersons = survivingMrnPersons.get(true);
        assertEquals(1, validSurvivingMrnPersons.size());
        // check the surviving MRN is marked as live
        PersonMrn validSurvivingMrnPerson = validSurvivingMrnPersons.get(0);
        assertTrue(validSurvivingMrnPerson.isLive());
        Person personForSurvivingMrn = validSurvivingMrnPerson.getPerson();

        Iterable<PersonMrn> allPersonMrn = personMrnRepo.findAll();
        for (PersonMrn pm : allPersonMrn) {
            System.out.println(String.format("ALL PERSON_MRN: pm: %s", pm));
            Mrn m = pm.getMrn();
            System.out.println(String.format("     MRN: m: %s", m));
            List<PersonMrn> persons = m.getPersons();
            for (PersonMrn pm_reverse : persons) {
                System.out.println(String.format("            PERSON_MRN: pm_reverse: %s", pm_reverse));
            }
        }

        System.out.println(String.format("------"));

        System.out.println(String.format("MRN: retiredMrn : %s", retiredMrn));
        System.out.println(String.format("MRN: survivingMrn     : %s", survivingMrn));
        System.out.println(String.format("PERSON_MRN: invalidRetiredMrnPerson: %s", invalidRetiredMrnPerson));
        System.out.println(String.format("PERSON_MRN: validSurvivingMrnPerson: %s", validSurvivingMrnPerson));
        System.out.println(String.format("PERSON: personForRetiredMrn      : %s", personForRetiredMrn));
        System.out.println(String.format("PERSON: personForSurvivingMrn: %s", personForSurvivingMrn));

        assertEquals(personForRetiredMrn, personForSurvivingMrn);

    }
}
