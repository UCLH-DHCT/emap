package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.DataSources;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditCoreDemographicRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditMrnToLiveRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnToLiveRepository;
import uk.ac.ucl.rits.inform.informdb.demographics.AuditCoreDemographic;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.identity.AuditMrnToLive;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;

/**
 * Interactions with patients at the person level: MRN and core demographics.
 * @author Stef Piatek
 */
@Component
public class PersonController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MrnRepository mrnRepo;
    private final MrnToLiveRepository mrnToLiveRepo;
    private final AuditMrnToLiveRepository auditMrnToLiveRepo;
    private final CoreDemographicRepository coreDemographicRepo;
    private final AuditCoreDemographicRepository auditCoreDemographicRepo;

    /**
     * Constructor implicitly autowiring beans.
     * @param mrnRepo                  mrnRepo
     * @param mrnToLiveRepo            mrnToLiveRepo
     * @param auditMrnToLiveRepo       auditMrnToLiveRepo
     * @param coreDemographicRepo      coreDemographicRepo
     * @param auditCoreDemographicRepo auditCoreDemographicRepo
     */
    public PersonController(MrnRepository mrnRepo, MrnToLiveRepository mrnToLiveRepo, AuditMrnToLiveRepository auditMrnToLiveRepo,
                            CoreDemographicRepository coreDemographicRepo, AuditCoreDemographicRepository auditCoreDemographicRepo) {
        this.mrnRepo = mrnRepo;
        this.mrnToLiveRepo = mrnToLiveRepo;
        this.auditMrnToLiveRepo = auditMrnToLiveRepo;
        this.coreDemographicRepo = coreDemographicRepo;
        this.auditCoreDemographicRepo = auditCoreDemographicRepo;
    }

    /**
     * Merge MRN from the message's pervious MRN into the surviving MRN.
     * @param msg          Merge message
     * @param survivingMrn live MRN to merge into
     * @param storedFrom   when the message has been read by emap core
     * @throws MessageIgnoredException if no retiring mrn information
     */
    @Transactional
    public void mergeMrns(final MergePatient msg, final Mrn survivingMrn, final Instant storedFrom) throws MessageIgnoredException {
        // get original mrn objects by mrn or nhs number
        List<Mrn> originalMrns = mrnRepo
                .findAllByMrnOrNhsNumber(msg.getPreviousMrn(), msg.getPreviousNhsNumber())
                .orElseGet(() -> List.of(createNewLiveMrn(
                        msg.getPreviousMrn(), msg.getPreviousNhsNumber(), msg.getSourceSystem(), msg.getRecordedDateTime(), storedFrom)));
        // change all live mrns from original mrn to surviving mrn
        originalMrns.stream()
                .flatMap(mrn -> mrnToLiveRepo.getAllByLiveMrnIdEquals(mrn).stream())
                .forEach(mrnToLive -> updateMrnToLiveIfMessageIsNotBefore(survivingMrn, msg.getRecordedDateTime(), storedFrom, mrnToLive));
    }

    /**
     * Update MrnToLive with surviving MRN and log current state in audit table.
     * Only happens if live MRN id is different and the message date time is the same or later than the mrnToLive value.
     * @param survivingMrn    current live mrn
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @param mrnToLive       mrn to live entity
     */
    private void updateMrnToLiveIfMessageIsNotBefore(final Mrn survivingMrn, final Instant messageDateTime, final Instant storedFrom,
                                                     MrnToLive mrnToLive) {
        if (liveMrnIdIsDifferentAndMessageIsNotBefore(survivingMrn, messageDateTime, mrnToLive)) {
            logger.info("Merging previous MRN {} into surviving MRN {}", mrnToLive.getMrnId(), survivingMrn);
            // log current state to audit table and then update current row
            AuditMrnToLive audit = new AuditMrnToLive(mrnToLive, messageDateTime, storedFrom);
            auditMrnToLiveRepo.save(audit);
            mrnToLive.setLiveMrnId(survivingMrn);
        }
    }

    /**
     * @param survivingMrn    current live mrn
     * @param messageDateTime date time of the message
     * @param mrnToLive       mrn to live entity
     * @return true if mrn to live should be updated
     */
    private boolean liveMrnIdIsDifferentAndMessageIsNotBefore(Mrn survivingMrn, Instant messageDateTime, MrnToLive mrnToLive) {
        return !(mrnToLive.getLiveMrnId().getMrnId().equals(survivingMrn.getMrnId()) && mrnToLive.getValidFrom().isAfter(messageDateTime));
    }

    /**
     * Get an existing MRN or create a new one using the data provided.
     * @param mrnString       MRN
     * @param nhsNumber       NHS number
     * @param sourceSystem    source system
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @return The live MRN for the patient.
     */
    @Transactional
    public Mrn getOrCreateMrn(final String mrnString, final String nhsNumber, final String sourceSystem, final Instant messageDateTime,
                              final Instant storedFrom) {
        return mrnRepo
                .findByMrnOrNhsNumber(mrnString, nhsNumber)
                // mrn exists, get the live mrn
                .map(mrn1 -> mrnToLiveRepo.getByMrnIdEquals(mrn1).getLiveMrnId())
                // otherwise create new mrn and mrn_to_live row
                .orElseGet(() -> createNewLiveMrn(mrnString, nhsNumber, sourceSystem, messageDateTime, storedFrom));
    }

    /**
     * Update existing demographics if they have changed and are newer, otherwise create new demographics.
     * @param originalMrn     Id of the mrn
     * @param adtMessage      adt message
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     */
    @Transactional
    public void updateOrCreateDemographic(final Mrn originalMrn, final AdtMessage adtMessage, final Instant messageDateTime,
                                          final Instant storedFrom) {
        coreDemographicRepo
                .getByMrnIdEquals(originalMrn)
                .map(demo -> new RowState<>(demo, messageDateTime, storedFrom, false))
                .map(demoState -> updateDemographicsIfNewer(adtMessage, demoState))
                .orElseGet(() -> createCoreDemographic(originalMrn, adtMessage, messageDateTime, storedFrom));
    }

    /**
     * Create new core demographic and save it.
     * @param originalMrn     Id of the mrn
     * @param adtMessage      adt message
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @return CoreDemographic from the message.
     */
    private CoreDemographic createCoreDemographic(Mrn originalMrn, AdtMessage adtMessage, Instant messageDateTime, Instant storedFrom) {
        RowState<CoreDemographic> demoState = new RowState<>(new CoreDemographic(originalMrn), messageDateTime, storedFrom, true);
        updateCoreDemographicFields(adtMessage, demoState);
        logger.info("Creating new core demographics {}", demoState.getEntity());
        return coreDemographicRepo.save(demoState.getEntity());
    }

    /**
     * Updates demographics if newer and different, logging original version in audit table.
     * @param adtMessage adt message
     * @param demoState  core demographics from the database that may be updated
     * @return existing demographic, with fields updated if relevant
     */
    private CoreDemographic updateDemographicsIfNewer(final AdtMessage adtMessage, RowState<CoreDemographic> demoState) {
        CoreDemographic originalDemo = demoState.getEntity().copy();
        if (shouldUpdateMessage(adtMessage, originalDemo)) {
            updateCoreDemographicFields(adtMessage, demoState);

            if (demoState.isEntityUpdated()) {
                logger.debug("Updating core demographics {}", demoState.getEntity());
                AuditCoreDemographic audit = new AuditCoreDemographic(originalDemo, demoState.getMessageDateTime(), demoState.getStoredFrom());
                auditCoreDemographicRepo.save(audit);
            }

        }
        return demoState.getEntity();
    }

    /**
     * ADT message is newer than database information, and the message source is trusted.
     * @param adtMessage          adt message
     * @param existingDemographic core demographics from the database
     * @return true if the demographics should be updated
     */
    private boolean shouldUpdateMessage(final AdtMessage adtMessage, final CoreDemographic existingDemographic) {
        return existingDemographic.getValidFrom().isBefore(adtMessage.getRecordedDateTime()) && DataSources.isTrusted(adtMessage.getSourceSystem());
    }

    /**
     * Update core demographics fields from known values of the adt message.
     * Tracks if the entity was updated from the known information.
     * @param adtMessage       adt message
     * @param demographicState state for the demographic entity
     */
    private void updateCoreDemographicFields(final AdtMessage adtMessage, RowState<CoreDemographic> demographicState) {
        CoreDemographic demo = demographicState.getEntity();
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientGivenName(), demo.getFirstname(), demo::setFirstname);
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientMiddleName(), demo.getMiddlename(), demo::setMiddlename);
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientFamilyName(), demo.getLastname(), demo::setLastname);
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientBirthDate(), demo.getDateOfBirth(), demo::setDateOfBirth);
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientBirthDate(), demo.getDatetimeOfBirth(), demo::setDatetimeOfBirth);
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientSex(), demo.getSex(), demo::setSex);
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientZipOrPostalCode(), demo.getHomePostcode(), demo::setHomePostcode);
        // death
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientIsAlive(), demo.isAlive(), demo::setAlive);
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientDeathDateTime(), demo.getDateOfDeath(), demo::setDateOfDeath);
        demographicState.assignHl7ValueIfDifferent(adtMessage.getPatientDeathDateTime(), demo.getDatetimeOfDeath(), demo::setDatetimeOfDeath);
    }

    /**
     * Create new Mrn and MrnToLive.
     * @param mrnString       MRN
     * @param nhsNumber       NHS number
     * @param sourceSystem    source system
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @return new MRN
     */
    private Mrn createNewLiveMrn(final String mrnString, final String nhsNumber, final String sourceSystem,
                                 final Instant messageDateTime,
                                 final Instant storedFrom) {
        logger.info(String.format("Creating new MRN (mrn=%s, nhsNumber=%s)", mrnString, nhsNumber));
        Mrn mrn = new Mrn();
        mrn.setMrn(mrnString);
        mrn.setNhsNumber(nhsNumber);
        mrn.setSourceSystem(sourceSystem);
        mrn.setStoredFrom(storedFrom);

        MrnToLive mrnToLive = new MrnToLive();
        mrnToLive.setMrnId(mrn);
        mrnToLive.setLiveMrnId(mrn);
        mrnToLive.setStoredFrom(storedFrom);
        mrnToLive.setValidFrom(messageDateTime);
        mrnToLiveRepo.save(mrnToLive);
        return mrn;
    }

    /**
     * Deletes the core demographic if the message date time is newer than the database.
     * @param mrn             MRN
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     */
    public void deleteDemographic(final Mrn mrn, final Instant messageDateTime, final Instant storedFrom) {
        coreDemographicRepo.getByMrnIdEquals(mrn).ifPresentOrElse(
                demo -> deleteIfMessageIsNewer(demo, messageDateTime, storedFrom),
                () -> logger.warn("No demographics to delete for for mrn: {} ", mrn)
        );
    }

    /**
     * Delete the core demographic if the message date time is newer than the database.
     * @param demo            core demographics entity
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     */
    private void deleteIfMessageIsNewer(CoreDemographic demo, Instant messageDateTime, Instant storedFrom) {
        if (messageDateTime.isAfter(demo.getValidFrom())) {
            AuditCoreDemographic audit = new AuditCoreDemographic(demo, messageDateTime, storedFrom);
            auditCoreDemographicRepo.save(audit);
            coreDemographicRepo.delete(demo);
        }
    }

    /**
     * Update the patient identifiers for an MRN. Because new MRN doesn't already exist, this is a modify instead of a merge.
     * <p>
     * It looks very rare that this is done, neonates and unusual cases where there are only a few ADT messages, no other result types
     * found so it should be fine even if mid-stream.
     * @param msg             ChangePatientIdentifiers
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @return MRN with the correct identifiers
     */
    @Transactional
    public Mrn updatePatientIdentifiersOrCreateMrn(ChangePatientIdentifiers msg, Instant messageDateTime, Instant storedFrom) {
        if (mrnExists(msg.getMrn())) {
            throw new IllegalArgumentException(String.format("New MRN can't already exist for a ChangePatientIdentifier message: %s", msg));
        }
        Mrn mrn = getOrCreateMrn(msg.getPreviousMrn(), msg.getPreviousNhsNumber(), msg.getSourceSystem(), messageDateTime, storedFrom);

        mrn.setMrn(msg.getMrn());
        if (msg.getNhsNumber() != null) {
            mrn.setNhsNumber(msg.getNhsNumber());
        }
        return mrn;
    }

    /**
     * @param mrn mrn string
     * @return true if an MRN exists by the mrn string
     */
    private boolean mrnExists(String mrn) {
        return mrnRepo.getByMrnEquals(mrn).isPresent();
    }
}
