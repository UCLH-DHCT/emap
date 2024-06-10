package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitObservationController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.WaveformController;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.visit_observations.FlowsheetMetadata;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;

/**
 * Handle processing of Flowsheet messages.
 * @author Stef Piatek
 */
@Component
public class WaveformProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonController personController;
    private final VisitController visitController;
    private final VisitObservationController visitObservationController;
    private final WaveformController waveformController;

    /**
     * @param personController           person controller.
     * @param visitController            visit controller
     * @param visitObservationController visit observation controller
     */
    public WaveformProcessor(
            PersonController personController, VisitController visitController, VisitObservationController visitObservationController,
            WaveformController waveformController) {
        this.personController = personController;
        this.visitController = visitController;
        this.visitObservationController = visitObservationController;
        this.waveformController = waveformController;
    }

    /**
     * Process flowsheet message.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final WaveformMessage msg, final Instant storedFrom) throws EmapOperationMessageProcessingException {
        waveformController.processWaveform(msg, storedFrom);
    }

    /**
     * Process flowsheet metadata.
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final FlowsheetMetadata msg, final Instant storedFrom) throws EmapOperationMessageProcessingException {
        visitObservationController.processMetadata(msg, storedFrom);
    }
}
