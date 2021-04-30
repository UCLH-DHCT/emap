package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateTypeRepository;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.io.IOException;
import java.util.List;

public class TestPatientInfectionProcessing extends MessageProcessingBase {
    @Autowired
    PatientStateRepository patientStateRepository;
    @Autowired
    PatientStateTypeRepository patientStateTypeRepository;

    List<PatientInfection> hooverMessages;
    PatientInfection hl7Mumps;
    PatientInfection hooverMumps;

    @BeforeEach
    private void setUp() throws IOException {
        hooverMessages = messageFactory.getPatientInfections("2019-04.yaml");
        hl7Mumps = messageFactory.getPatientInfections("hl7/minimal_mumps.yaml").get(0);
        hooverMumps = hooverMessages.get(0);
    }


}
