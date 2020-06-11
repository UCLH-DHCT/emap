package uk.ac.ucl.rits.inform.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;

/**
 * Messages come through giving progressively more results.
 * New results should be added taking care to avoid duplicating existing ones.
 * @author Jeremy Stein
 */
public class TestIncrementalPathology extends Hl7StreamEndToEndTestCase {
    public TestIncrementalPathology() {
        super();
        hl7StreamFileNames.add("IncrementalPathology.txt");
    }

    @Test
    @Transactional
    public void testSomething() {
        System.out.println(emapStarTestUtils.prettyPrintEncounterFacts("123412341234"));

        List<PatientFact> order001 = patientFactRepo.findAllPathologyOrdersByOrderNumber("94000001");
        List<PatientFact> childFacts001 = order001.get(0).getChildFacts();
        List<PatientFact> order002 = patientFactRepo.findAllPathologyOrdersByOrderNumber("94000002");
        List<PatientFact> childFacts002 = order002.get(0).getChildFacts();

        // check that pathology results have not been duplicated
        List<PatientFact> allTestResults001 = childFacts001.stream().filter(pf -> pf.isValid() && pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        List<PatientFact> allTestResults002 = childFacts002.stream().filter(pf -> pf.isValid() && pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)).collect(Collectors.toList());
        assertEquals(2, allTestResults001.size());
        assertEquals(17, allTestResults002.size());
    }

    /**
     * Given that two messages give the monocyte result, the result from the last message to be encountered should be used
     */
    @Test
    @Transactional
    public void testResultsUpdate() {
        List<PatientFact> findAllPathologyOrdersByOrderNumber = patientFactRepo.findAllPathologyOrdersByOrderNumber("94000002");
        assertEquals(1, findAllPathologyOrdersByOrderNumber.size());
        List<PatientFact> resultFacts = findAllPathologyOrdersByOrderNumber.get(0).getChildFacts();
        Map<String, List<PatientFact>> resultFactsByTestCode = resultFacts.stream()
                .filter(pf -> pf.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT))
                .collect(Collectors.groupingBy(
                        pf -> pf.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_CODE)
                                .get(0).getValueAsString()));
        List<PatientFact> monocyteResults = resultFactsByTestCode.get("MO");
        assertEquals(3, monocyteResults.size());
        Map<Pair<Boolean, Boolean>, List<PatientFact>> monocyteResultsByValidity = monocyteResults.stream().collect(Collectors
                .groupingBy(res -> new ImmutablePair<>(res.getValidUntil() == null, res.getStoredUntil() == null)));
        List<PatientFact> validResults = monocyteResultsByValidity.get(new ImmutablePair<>(true, true));
        assertEquals(1, validResults.size());
        List<PatientProperty> monocyteProperties = validResults.get(0).getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_NUMERIC_VALUE);
        assertEquals(1, monocyteProperties.size());
        // first message has result of 0.35, second message has a result of 0.5 so this should be used
        assertEquals(0.5, monocyteProperties.get(0).getValueAsReal());
    }
}
