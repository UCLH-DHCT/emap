package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.model.v26.message.ADT_A05;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

import java.util.Collection;

@Component
public class PatientStatusService {
    public Collection<? extends EmapOperationMessage> buildMessages(String sourceId, ADT_A05 msg) {
        return null;
    }
}
