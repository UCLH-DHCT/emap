package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.stream.Stream;

class TestAdtProcessingUnorderedLocation extends MessageProcessingBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    OrderPermutationTestProducer orderPermutationTestProducer;

    /**
     * Create all the tests.
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    public Stream<DynamicTest> testUnorderedMoves() {
        orderPermutationTestProducer.setMessagePath("location");
        orderPermutationTestProducer.setInitialAdmissionTime(Instant.parse("2013-02-11T11:00:52Z"));
        orderPermutationTestProducer.setAdtFilenames(new String[]{"02_A01", "03_A02", "04_A02", "05_A02", "06_A02", "07_A06", "08_A03"});
        orderPermutationTestProducer.setLocations(new String[]{
                "ED^UCHED RAT CHAIR^RAT-CHAIR",
                "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
                "ED^NON COVID MAJORS 04^04-NON COVID MAJORS",
                "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
                "ED^NON COVID MAJORS 04^04-NON COVID MAJORS",
                "EAU^UCH T00 EAU BY02^BY02-08",
        });
        return orderPermutationTestProducer.testUnorderedMessages();
    }


}
