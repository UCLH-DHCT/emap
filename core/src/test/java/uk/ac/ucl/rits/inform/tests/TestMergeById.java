package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonMrnRepository;
import uk.ac.ucl.rits.inform.informdb.Mrn;
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

        // Surviving MRN is simpler as it shouldn't have changed - test this first.
        // the surviving MRN should retain its single, valid, live link to the person
        List<PersonMrn> survivingMrnPersons = survivingMrn.getPersons();
        assertEquals(1, survivingMrnPersons.size());
        assertTrue(survivingMrnPersons.get(0).isValid());
        assertTrue(survivingMrnPersons.get(0).isLive());

        // Retired MRN has three rows in total
        Mrn retiredMrn = mrnRepo.findByMrnString("40800000");
        Map<Pair<Boolean, Boolean>, List<PersonMrn>> retiredMrnPersons = retiredMrn.getPersons().stream()
                .collect(Collectors.groupingBy(p -> new ImmutablePair<>(p.getStoredUntil() == null, p.getValidUntil() == null)));

        assertFalse(retiredMrnPersons.containsKey(new ImmutablePair<>(false, false)));
        // There is a stored and valid link between the retired MRN and its new
        // person, but it's not that person's live MRN
        List<PersonMrn> storedValidRetiredMrnPersons = retiredMrnPersons.get(new ImmutablePair<>(true, true));
        assertEquals(1, storedValidRetiredMrnPersons.size(), "retired mrn should still have a valid connection to a person");
        assertFalse(storedValidRetiredMrnPersons.get(0).isLive());

        // The link between the retired MRN and its old person is no longer true, so the old row has been deleted
        // and replaced with a new row showing the invalidation date
        List<PersonMrn> deletedValidRetiredMrnPersons = retiredMrnPersons.get(new ImmutablePair<>(false, true));
        List<PersonMrn> storedInvalidRetiredMrnPersons = retiredMrnPersons.get(new ImmutablePair<>(true, false));
        assertEquals(1, deletedValidRetiredMrnPersons.size());
        assertEquals(1, storedInvalidRetiredMrnPersons.size());
        // Both rows are still live to reflect the state that was true in the past
        assertTrue(deletedValidRetiredMrnPersons.get(0).isLive());
        assertTrue(storedInvalidRetiredMrnPersons.get(0).isLive());

        // Check the MRNs point to the right persons
        assertEquals(storedInvalidRetiredMrnPersons.get(0).getPerson(), deletedValidRetiredMrnPersons.get(0).getPerson());
        assertEquals(storedValidRetiredMrnPersons.get(0).getPerson(), survivingMrnPersons.get(0).getPerson());
        assertNotEquals(storedValidRetiredMrnPersons.get(0).getPerson(), deletedValidRetiredMrnPersons.get(0).getPerson());
    }
}
