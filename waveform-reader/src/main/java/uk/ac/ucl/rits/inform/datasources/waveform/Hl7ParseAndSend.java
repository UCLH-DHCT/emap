package uk.ac.ucl.rits.inform.datasources.waveform;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import ca.uhn.hl7v2.parser.CustomModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class Hl7ParseAndSend {
    private final Logger logger = LoggerFactory.getLogger(Hl7ParseAndSend.class);

    private final WaveformOperations waveformOperations;

    Hl7ParseAndSend(WaveformOperations waveformOperations) {
        this.waveformOperations = waveformOperations;
    }

    private HapiContext hapiContext = initializeHapiContext();

    private static HapiContext initializeHapiContext() {
        HapiContext context = new DefaultHapiContext();
        context.setValidationContext(ValidationContextFactory.noValidation());
        ModelClassFactory mcf = new CustomModelClassFactory() {
            @Override
            public Class<? extends Message> getMessageClass(String theName, String theVersion, boolean isExplicit) throws HL7Exception {
                /* Tell HAPI to create message classes of a specific version, otherwise you seem to get GenericMessage
                 * and GenericSegment, which are not castable to more useful classes like ORU_R01 and MSH.
                 */
                return super.getMessageClass(theName, "2.6", isExplicit);
            }

            @Override
            public Class<? extends Group> getGroupClass(String name, String version) throws HL7Exception {
                return super.getGroupClass(name, "2.6");
            }
        };
        context.setModelClassFactory(mcf);
        return context;
    }

    // XXX: this will have to be returning some kind of incomplete message form because
    // we will be merging messages so they're big enough to prevent performance/storage
    // problems in the core proc/DB
    List<WaveformMessage> parseHl7(String messageAsStr) throws HL7Exception {
        logger.info("Parsing message of size {}", messageAsStr.length());
        logger.trace("Full HL7 message:\n {}", messageAsStr);
        Instant start = Instant.now();
        PipeParser parser = hapiContext.getPipeParser();
        ORU_R01 hl7Message = (ORU_R01) parser.parse(messageAsStr).getMessage();
        MSH msh = (MSH) hl7Message.get("MSH");
        String triggerEvent = msh.getMsh9_MessageType().getMsg2_TriggerEvent().getValueOrEmpty();
        String messageCode = msh.getMsh9_MessageType().getMsg1_MessageCode().getValueOrEmpty();
        logger.debug("message of type {}^{}: ", messageCode, triggerEvent);
        String messageIdBase = msh.getMsh10_MessageControlID().getValueOrEmpty();

        // XXX: probably need to look this up!
        Long samplingRate = Long.parseLong("42");

        /*
         * The NA class seems to be a bit weird in that it only supports 4 fields out of the
         * 65535 allowed.
         * So use Terser, but we still need to know how many repeats there are of the
         * enclosing structures.
         * So far I have only seen messages with a single OBR containing one or more OBX but
         * in principle there could be more.
         */
        List<WaveformMessage> allWaveformMessages = new ArrayList<>();
        List<ORU_R01_PATIENT_RESULT> patientResults = hl7Message.getPATIENT_RESULTAll();
        logger.debug("ORU_R01_PATIENT_RESULT count = {}", patientResults.size());
        for (int prI = 0; prI < patientResults.size(); prI++) {
            ORU_R01_PATIENT_RESULT pr = patientResults.get(prI);
            PV1 pv1 = pr.getPATIENT().getVISIT().getPV1();
            String patientlocation = pv1.getPv13_AssignedPatientLocation().getPl1_PointOfCare().getValueOrEmpty();
            List<ORU_R01_ORDER_OBSERVATION> orderObservations = pr.getORDER_OBSERVATIONAll();

            logger.debug("ORU_R01_ORDER_OBSERVATION count = {}", orderObservations.size());
            for (int ooI = 0; ooI < orderObservations.size(); ooI++) {
                ORU_R01_ORDER_OBSERVATION oo = orderObservations.get(ooI);
                OBR obr = oo.getOBR();
                String locationId = obr.getObr10_CollectorIdentifier(0).getXcn1_IDNumber().getValueOrEmpty();
                if (!locationId.equals(patientlocation)) {
                    // XXX: make our own exception
                    throw new HL7Exception("Unexpected location " + locationId + "|" + patientlocation);
                }
                List<ORU_R01_OBSERVATION> observations = oo.getOBSERVATIONAll();
                logger.debug("ORU_R01_OBSERVATION count = {}", observations.size());
                for (int obsI = 0; obsI < observations.size(); obsI++) {
                    ORU_R01_OBSERVATION obs = observations.get(obsI);
                    OBX obx = obs.getOBX();
                    String streamId = obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty();
                    Instant obsDatetime = obx.getObx14_DateTimeOfTheObservation().getValueAsCalendar().toInstant();

                    List<Double> arrayDoubles = new ArrayList<>();
                    /* Repeats are separated by "~", components by "^".
                     * I haven't seen a message that uses repeats. Rather than try and fudge it,
                     * throw an exception so I can investigate what such a message actually means
                     */
                    int observationValueReps = obx.getObservationValueReps();
                    if (observationValueReps != 1) {
                        throw new HL7Exception("must only be 1 repeat in OBX-5, got " + observationValueReps);
                    }
                    // HAPI doesn't seem to be able to tell us how many values are in an array, so detect the end
                    int observationValueMaxValues = 65536;
                    for (int vI = 0; vI < observationValueMaxValues; vI++) {
                        // This flavour of "get" is faster than using the Terser's string-based location spec.
                        // HL7 fields, components, and sub-components are 1-indexed but
                        // HAPI groups and HL7 repeats are 0-indexed.
                        String s = Terser.get(obx, 5, 0, vI + 1, 1);
                        if (s == null) {
                            break;
                        }
                        arrayDoubles.add(Double.parseDouble(s));
                    }
                    // one HL7 message can create more than one message, so disambiguate
                    String messageIdSpecific = String.format("%s_%d_%d_%d", messageIdBase, prI, ooI, obsI);
                    logger.debug("location {}, time {}, messageId {}, value count = {}",
                            locationId, obsDatetime, messageIdSpecific, arrayDoubles.size());
                    WaveformMessage waveformMessage = waveformMessageFromValues(
                            samplingRate, locationId, obsDatetime, messageIdSpecific, streamId, arrayDoubles);
                    allWaveformMessages.add(waveformMessage);
                }
            }
        }
        Instant afterParse = Instant.now();
        logger.info("Timing: message length {}, parse {} ms",
                messageAsStr.length(),
                start.until(afterParse, ChronoUnit.MILLIS));
        return allWaveformMessages;
    }

    private WaveformMessage waveformMessageFromValues(
            Long samplingRate, String locationId, Instant messageStartTime, String messageId,
            String sourceStreamId, List<Double> arrayValues) {
        WaveformMessage waveformMessage = new WaveformMessage();
        waveformMessage.setSamplingRate(samplingRate);
        waveformMessage.setSourceLocationString(locationId);
        // XXX: need to perform location mapping here and set the mapped location
        // XXX: ditto stream ID mapping
        waveformMessage.setObservationTime(messageStartTime);
        waveformMessage.setSourceMessageId(messageId);
        waveformMessage.setSourceStreamId(sourceStreamId);
        waveformMessage.setNumericValues(new InterchangeValue<>(arrayValues));
        logger.trace("output interchange waveform message = {}", waveformMessage);
        return waveformMessage;
    }

    /**
     * Parse and publish an HL7 message.
     * @param messageAsStr One HL7 message as a string
     * @throws InterruptedException if publisher send is interrupted
     * @throws HL7Exception if HAPI does
     */
    public void parseAndSend(String messageAsStr) throws InterruptedException, HL7Exception {
        List<WaveformMessage> msgs = parseHl7(messageAsStr);

        logger.info("HL7 message generated {} Waveform messages, sending", msgs.size());
        for (var m: msgs) {
            // consider sending to publisher in batches?
            waveformOperations.sendMessage(m);
        }
    }
}
