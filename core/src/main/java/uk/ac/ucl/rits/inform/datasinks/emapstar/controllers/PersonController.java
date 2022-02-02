package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.DataSources;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnToLiveAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnToLiveRepository;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographicAudit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLiveAudit;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Interactions with patients at the person level: MRN and core demographics.
 * @author Stef Piatek
 */
@Component
public class PersonController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MrnRepository mrnRepo;
    private final MrnToLiveRepository mrnToLiveRepo;
    private final MrnToLiveAuditRepository mrnToLiveAuditRepo;
    private final CoreDemographicRepository coreDemographicRepo;
    private final CoreDemographicAuditRepository coreDemographicAuditRepo;

    /**
     * Constructor implicitly autowiring beans.
     * @param mrnRepo                  mrnRepo
     * @param mrnToLiveRepo            mrnToLiveRepo
     * @param mrnToLiveAuditRepo       auditMrnToLiveRepo
     * @param coreDemographicRepo      coreDemographicRepo
     * @param coreDemographicAuditRepo auditCoreDemographicRepo
     */
    public PersonController(MrnRepository mrnRepo, MrnToLiveRepository mrnToLiveRepo, MrnToLiveAuditRepository mrnToLiveAuditRepo,
                            CoreDemographicRepository coreDemographicRepo, CoreDemographicAuditRepository coreDemographicAuditRepo) {
        this.mrnRepo = mrnRepo;
        this.mrnToLiveRepo = mrnToLiveRepo;
        this.mrnToLiveAuditRepo = mrnToLiveAuditRepo;
        this.coreDemographicRepo = coreDemographicRepo;
        this.coreDemographicAuditRepo = coreDemographicAuditRepo;
    }

    /**
     * Merge MRN from the message's pervious MRN into the surviving MRN.
     * @param msg          Merge message
     * @param survivingMrn live MRN to merge into
     * @param storedFrom   when the message has been read by emap core
     * @throws RequiredDataMissingException if mrn and nhsNumber are both null
     */
    @Transactional
    public void mergeMrns(final MergePatient msg, final Mrn survivingMrn, final Instant storedFrom) throws RequiredDataMissingException {
        // get original mrn objects by mrn or nhs number
        List<Mrn> originalMrns = getMrnsOrCreateOne(
                msg.getPreviousMrn(), msg.getPreviousNhsNumber(), msg.getSourceSystem(), msg.bestGuessAtValidFrom(), storedFrom
        );
        mergeMrns(originalMrns, survivingMrn, msg.bestGuessAtValidFrom(), storedFrom);
    }

    private List<Mrn> getMrnsOrCreateOne(String mrn, String nhsNumber, String sourceSystem, Instant validFrom, Instant storedFrom)
            throws RequiredDataMissingException {
        List<Mrn> originalMrns = mrnRepo
                .findAllByMrnOrNhsNumber(mrn, nhsNumber);
        if (originalMrns.isEmpty()) {
            originalMrns = Collections.singletonList(
                    createNewLiveMrn(mrn, nhsNumber, sourceSystem, validFrom, storedFrom)
            );
        }
        return originalMrns;
    }

    private void mergeMrns(Collection<Mrn> originalMrns, Mrn survivingMrn, Instant validFrom, Instant storedFrom) {
        // change all live mrns from original mrn to surviving mrn
        originalMrns.stream()
                .flatMap(mrn -> mrnToLiveRepo.getAllByLiveMrnIdEquals(mrn).stream())
                .forEach(mrnToLive -> updateMrnToLiveIfMessageIsNotBefore(survivingMrn, validFrom, storedFrom, mrnToLive));
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
            logger.debug("Merging previous MRN {} into surviving MRN {}", mrnToLive.getMrnId(), survivingMrn);
            // log current state to audit table and then update current row
            MrnToLiveAudit audit = new MrnToLiveAudit(mrnToLive, messageDateTime, storedFrom);
            mrnToLiveAuditRepo.save(audit);
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
     * @throws RequiredDataMissingException If MRN and NHS number are both null
     */
    @Transactional
    public Mrn getOrCreateMrn(final String mrnString, final String nhsNumber, final String sourceSystem, final Instant messageDateTime,
                              final Instant storedFrom) throws RequiredDataMissingException {
        logger.debug("Getting or creating MRN: mrn {}, nhsNumber {}", mrnString, nhsNumber);
        return mrnRepo
                .findByMrnOrNhsNumber(mrnString, nhsNumber)
                .map(mrn -> updateIdentfiersAndGetLiveMrn(sourceSystem, mrnString, nhsNumber, mrn))
                // otherwise create new mrn and mrn_to_live row
                .orElseGet(() -> createNewLiveMrn(mrnString, nhsNumber, sourceSystem, messageDateTime, storedFrom));
    }

    /**
     * Update identifiers (nhs number if different, MRN if missing), then return current live MRN.
     * @param sourceSystem source system
     * @param mrnString    MRN string
     * @param nhsNumber    NHS number
     * @param mrn          MRN entity
     * @return the live MRN entity
     */
    private Mrn updateIdentfiersAndGetLiveMrn(final String sourceSystem, final String mrnString, final String nhsNumber, Mrn mrn) {
        if (DataSources.isTrusted(sourceSystem)) {
            if (nhsNumber != null && !nhsNumber.equals(mrn.getNhsNumber())) {
                logger.debug("Updating NHS number to {} for MRN {}", nhsNumber, mrn);
                mrn.setNhsNumber(nhsNumber);
            }
            // Only update the MRN if we have an orphan NHS number with no MRN
            if (mrnString != null && mrn.getMrn() == null && mrn.getNhsNumber() != null) {
                logger.debug("Updating mrn to {} for MRN {}", mrnString, mrn);
                mrn.setMrn(mrnString);
            }
        }

        return mrnToLiveRepo.getByMrnIdEquals(mrn).getLiveMrnId();
    }


    /**
     * Get or create MRN using only the MRN string for the get.
     * @param mrnString       MRN
     * @param nhsNumber       NHS number
     * @param sourceSystem    source system
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @return The live MRN for the patient.
     * @throws RequiredDataMissingException If MRN is null
     */
    @Transactional
    public Mrn getOrCreateOnMrnOnly(String mrnString, String nhsNumber, String sourceSystem, Instant messageDateTime, Instant storedFrom)
            throws RequiredDataMissingException {
        if (mrnString == null) {
            throw new RequiredDataMissingException("No MRN found");
        }
        logger.debug("Getting or creating MRN: mrn {} only", mrnString);
        return mrnRepo
                .findByMrnEquals(mrnString)
                // mrn exists, update NHS number if message source is trusted, then get the live mrn
                .map(mrn -> updateIdentfiersAndGetLiveMrn(sourceSystem, mrnString, nhsNumber, mrn))
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
        RowState<CoreDemographic, CoreDemographicAudit> demoState = new RowState<>(
                new CoreDemographic(originalMrn), messageDateTime, storedFrom, true);
        updateCoreDemographicFields(adtMessage, demoState);
        demoState.saveEntityOrAuditLogIfRequired(coreDemographicRepo, coreDemographicAuditRepo);
        return demoState.getEntity();
    }

    /**
     * Updates demographics if newer and different, logging original version in audit table.
     * @param adtMessage adt message
     * @param demoState  core demographics from the database that may be updated
     * @return existing demographic, with fields updated if relevant
     */
    private CoreDemographic updateDemographicsIfNewer(final AdtMessage adtMessage, RowState<CoreDemographic, CoreDemographicAudit> demoState) {
        if (shouldUpdateMessage(adtMessage, demoState.getEntity())) {
            updateCoreDemographicFields(adtMessage, demoState);
            demoState.saveEntityOrAuditLogIfRequired(coreDemographicRepo, coreDemographicAuditRepo);
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
        return existingDemographic.getValidFrom().isBefore(adtMessage.bestGuessAtValidFrom()) && DataSources.isTrusted(adtMessage.getSourceSystem());
    }

    /**
     * Update core demographics fields from known values of the adt message.
     * Tracks if the entity was updated from the known information.
     * @param adtMessage       adt message
     * @param demographicState state for the demographic entity
     */
    private void updateCoreDemographicFields(final AdtMessage adtMessage, RowState<CoreDemographic, CoreDemographicAudit> demographicState) {
        CoreDemographic demo = demographicState.getEntity();
        demographicState.assignInterchangeValue(adtMessage.getPatientGivenName(), demo.getFirstname(), demo::setFirstname);
        demographicState.assignInterchangeValue(adtMessage.getPatientMiddleName(), demo.getMiddlename(), demo::setMiddlename);
        demographicState.assignInterchangeValue(adtMessage.getPatientFamilyName(), demo.getLastname(), demo::setLastname);
        demographicState.assignInterchangeValue(adtMessage.getPatientBirthDate(), demo.getDateOfBirth(), demo::setDateOfBirth);
        demographicState.assignInterchangeValue(adtMessage.getPatientBirthDateTime(), demo.getDatetimeOfBirth(), demo::setDatetimeOfBirth);
        demographicState.assignInterchangeValue(adtMessage.getPatientSex(), demo.getSex(), demo::setSex);
        demographicState.assignInterchangeValue(adtMessage.getPatientZipOrPostalCode(), demo.getHomePostcode(), demo::setHomePostcode);
        demographicState.assignInterchangeValue(adtMessage.getEthnicGroup(), demo.getEthnicity(), demo::setEthnicity);
        // death
        demographicState.assignInterchangeValue(adtMessage.getPatientIsAlive(), demo.getAlive(), demo::setAlive);
        demographicState.assignInterchangeValue(adtMessage.getPatientDeathDateTime(), demo.getDateOfDeath(), demo::setDateOfDeath);
        demographicState.assignInterchangeValue(adtMessage.getPatientDeathDateTime(), demo.getDatetimeOfDeath(), demo::setDatetimeOfDeath);
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
            CoreDemographicAudit audit = new CoreDemographicAudit(demo, messageDateTime, storedFrom);
            coreDemographicAuditRepo.save(audit);
            coreDemographicRepo.delete(demo);
        }
    }

    /**
     * Update the patient identifiers for an MRN.
     * <p>
     * With messages out of order and difference sources of information, we have had to alter the way we process these messages.
     * - If the surviving MRN doesn't already exist then we modify the previous MRN (as per HL7 specification).
     * - If MRNs matching the surviving or previous identifiers are from untrusted sources, merge the MRNs.
     * @param msg             ChangePatientIdentifiers
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @throws IncompatibleDatabaseStateException if an MRN already exists
     * @throws RequiredDataMissingException       If MRN and NHS number are both null
     */
    @Transactional
    public void updatePatientIdentifiersOrCreateMrn(ChangePatientIdentifiers msg, Instant messageDateTime, Instant storedFrom)
            throws IncompatibleDatabaseStateException, RequiredDataMissingException {
        List<Mrn> survivingMrns = mrnRepo.findAllByMrnOrNhsNumber(msg.getMrn(), msg.getNhsNumber());
        List<Mrn> previousMrns = getMrnsOrCreateOne(
                msg.getPreviousMrn(), msg.getPreviousNhsNumber(), msg.getSourceSystem(), messageDateTime, storedFrom
        );
        // simple case, the surviving MRN doesn't exist so just update previous MRN with the new details
        if (survivingMrns.isEmpty()) {
            if (msg.getMrn() != null) {
                previousMrns.forEach(mrn -> mrn.setMrn(msg.getMrn()));
            }
            if (msg.getNhsNumber() != null) {
                previousMrns.forEach(mrn -> mrn.setNhsNumber(msg.getNhsNumber()));
            }
            logger.debug("Surviving MRN didn't already exist, so updated the previous MRN with the correct identifiers");
            return;
        }
        // surviving MRN exists should only be allowed if at least one of the surviving or previous MRNs are untrusted
        if (allMrnsTrusted(previousMrns) && allMrnsTrusted(survivingMrns)) {
            throw new IncompatibleDatabaseStateException(String.format("New MRN already exists: %s", msg));
        }
        // surviving Mrn should always have the correct live MRN so use the first one of these
        Mrn liveMrn = mrnToLiveRepo.getByMrnIdEquals(survivingMrns.get(0)).getLiveMrnId();

        mergeMrns(previousMrns, liveMrn, messageDateTime, storedFrom);
    }

    /**
     * All MRNs are from a trusted source.
     * @param mrns mrns
     * @return true if all MRNs are trusted
     */
    private boolean allMrnsTrusted(Collection<Mrn> mrns) {
        return mrns.stream()
                .allMatch(mrn -> DataSources.isTrusted(mrn.getSourceSystem()));
    }
}
