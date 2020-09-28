package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
     * Merge at least two MRNs, setting the surviving mrn to be live.
     * @param retiringMrn       MRN to retire and merge from
     * @param retiringNhsNumber nhsNumber to retire and merge from
     * @param survivingMrn      live MRN to merge into
     * @param messageDateTime   date time of the message
     * @param storedFrom        when the message has been read by emap core
     * @throws MessageIgnoredException if no retiring mrn information
     */
    @Transactional
    public void mergeMrns(final String retiringMrn, final String retiringNhsNumber, final Mrn survivingMrn,
                          final Instant messageDateTime, final Instant storedFrom) throws MessageIgnoredException {
        if (retiringMrn == null && retiringNhsNumber == null) {
            throw new MessageIgnoredException("Retiring MRN's Mrn string and NHS number were null");
        }
        // get original mrn objects by mrn or nhs number
        List<Mrn> originalMrns = mrnRepo
                .getAllByMrnIsNotNullAndMrnEqualsOrNhsNumberIsNotNullAndNhsNumberEquals(retiringMrn, retiringNhsNumber)
                .orElseGet(() -> List.of(createNewLiveMrn(retiringMrn, retiringNhsNumber, "EPIC", messageDateTime, storedFrom)));
        // change all live mrns from original mrn to surviving mrn
        originalMrns.stream()
                .flatMap(mrn -> mrnToLiveRepo.getAllByLiveMrnIdEquals(mrn).stream())
                .forEach(mrnToLive -> updateMrnToLiveIfMessageIsNotBefore(survivingMrn, messageDateTime, storedFrom, mrnToLive));
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
                .getByMrnEqualsOrMrnIsNullAndNhsNumberEquals(mrnString, nhsNumber)
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
                .map(existingDemographic -> updateDemographicsIfNewer(originalMrn, adtMessage, messageDateTime, storedFrom, existingDemographic))
                .orElseGet(() -> {
                    CoreDemographic messageDemographics = new CoreDemographic();
                    updateCoreDemographicFields(originalMrn, adtMessage, storedFrom, messageDemographics);
                    return coreDemographicRepo.save(messageDemographics);
                });
    }

    /**
     * Updates demographics if newer and different, logging original version in audit table.
     * @param originalMrn         The MRN
     * @param adtMessage          adt message
     * @param messageDateTime     date time of the message
     * @param storedFrom          when the message has been read by emap core
     * @param existingDemographic core demographics from the database that may be updated
     * @return existing demographic, with fields updated if relevant
     */
    private CoreDemographic updateDemographicsIfNewer(final Mrn originalMrn, final AdtMessage adtMessage, final Instant messageDateTime,
                                                      final Instant storedFrom, CoreDemographic existingDemographic) {
        if (messageIsDifferentAndIsNewer(originalMrn, adtMessage, storedFrom, existingDemographic)) {
            // log current state to audit table and then update current row
            AuditCoreDemographic auditCoreDemographic = new AuditCoreDemographic(existingDemographic, messageDateTime, storedFrom);
            auditCoreDemographicRepo.save(auditCoreDemographic);
            updateCoreDemographicFields(originalMrn, adtMessage, storedFrom, existingDemographic);
        }
        return existingDemographic;
    }

    /**
     * ADT message has different values and is newer than the existing core demographics.
     * @param originalMrn         The Mrn
     * @param adtMessage          adt message
     * @param storedFrom          when the message has been read by emap core
     * @param existingDemographic core demographics from the database
     * @return true if the demographics should be updated
     */
    private boolean messageIsDifferentAndIsNewer(final Mrn originalMrn, final AdtMessage adtMessage,
                                                 final Instant storedFrom, final CoreDemographic existingDemographic) {
        CoreDemographic messageDemographics = existingDemographic.copy();
        updateCoreDemographicFields(originalMrn, adtMessage, storedFrom, messageDemographics);
        return !existingDemographic.equals(messageDemographics) && existingDemographic.getValidFrom().isBefore(messageDemographics.getValidFrom());
    }

    /**
     * Update core demographics fields from known values of the adt message.
     * @param mrnId           Id of the mrn
     * @param adtMessage      adt message
     * @param storedFrom      when the message has been read by emap core
     * @param coreDemographic original core demographic object
     */
    private void updateCoreDemographicFields(final Mrn mrnId, final AdtMessage adtMessage, final Instant storedFrom,
                                             CoreDemographic coreDemographic) {
        coreDemographic.setMrnId(mrnId);
        adtMessage.getPatientGivenName().assignTo(coreDemographic::setFirstname);
        adtMessage.getPatientMiddleName().assignTo(coreDemographic::setMiddlename);
        adtMessage.getPatientFamilyName().assignTo(coreDemographic::setLastname);
        adtMessage.getPatientBirthDate().assignTo(instant -> coreDemographic.setDateOfBirth(convertToLocalDate(instant)));
        adtMessage.getPatientBirthDate().assignTo(coreDemographic::setDatetimeOfBirth);
        adtMessage.getPatientSex().assignTo(coreDemographic::setSex);
        adtMessage.getPatientZipOrPostalCode().assignTo(coreDemographic::setHomePostcode);
        // death
        adtMessage.getPatientIsAlive().assignTo(coreDemographic::setAlive);
        adtMessage.getPatientDeathDateTime().assignTo(instant -> coreDemographic.setDateOfDeath(convertToLocalDate(instant)));
        adtMessage.getPatientDeathDateTime().assignTo(coreDemographic::setDatetimeOfDeath);
        adtMessage.getPatientMiddleName().assignTo(coreDemographic::setMiddlename);
        // from dates
        coreDemographic.setStoredFrom(storedFrom);
        coreDemographic.setValidFrom(adtMessage.getRecordedDateTime());
    }

    /**
     * Convert instant to local date, allowing for nulls.
     * @param instant instant
     * @return LocalDate
     */
    private LocalDate convertToLocalDate(Instant instant) {
        return (instant == null) ? null : instant.atZone(ZoneId.systemDefault()).toLocalDate();
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
        logger.debug(String.format("Creating new MRN (mrn=%s, nhsNumber=%s)", mrnString, nhsNumber));
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
}
