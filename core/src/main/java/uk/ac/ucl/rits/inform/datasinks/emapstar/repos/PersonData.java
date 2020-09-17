package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
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
public class PersonData {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MrnRepository mrnRepo;
    private final MrnToLiveRepository mrnToLiveRepo;
    private final CoreDemographicRepository coreDemographicRepo;

    /**
     * Constructor implicitly autowiring beans.
     * @param mrnRepo             mrnRepo
     * @param mrnToLiveRepo       mrnToLiveRepo
     * @param coreDemographicRepo coreDemographicRepo
     */
    public PersonData(MrnRepository mrnRepo, MrnToLiveRepository mrnToLiveRepo, CoreDemographicRepository coreDemographicRepo) {
        this.mrnRepo = mrnRepo;
        this.mrnToLiveRepo = mrnToLiveRepo;
        this.coreDemographicRepo = coreDemographicRepo;
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
                .forEach(mrnToLive -> mrnToLive.setLiveMrnId(survivingMrn));
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
     * @param mrnId      Id of the mrn
     * @param adtMessage adt message
     * @param storedFrom when the message has been read by emap core
     */
    @Transactional
    public void updateOrCreateDemographic(final long mrnId, final AdtMessage adtMessage, final Instant storedFrom) {
        coreDemographicRepo
                .getByMrnIdEquals(mrnId)
                .map(existingDemographic -> {
                    if (messageIsDifferentAndIsNewer(mrnId, adtMessage, storedFrom, existingDemographic)) {
                        // log current state to audit table and then update current row
                        updateCoreDemographicFields(mrnId, adtMessage, storedFrom, existingDemographic);
                    }
                    return existingDemographic;
                })
                .orElseGet(() -> {
                    CoreDemographic messageDemographics = new CoreDemographic();
                    updateCoreDemographicFields(mrnId, adtMessage, storedFrom, messageDemographics);
                    return coreDemographicRepo.save(messageDemographics);
                });
    }

    /**
     * ADT message has different values and is newer than the existing core demographics.
     * @param mrnId               Id of the mrn
     * @param adtMessage          adt message
     * @param storedFrom          when the message has been read by emap core
     * @param existingDemographic core demographics from the database
     * @return true if the demographics should be updated
     */
    private boolean messageIsDifferentAndIsNewer(final long mrnId, final AdtMessage adtMessage,
                                                 final Instant storedFrom, CoreDemographic existingDemographic) {
        CoreDemographic messageDemographics = existingDemographic.copy();
        updateCoreDemographicFields(mrnId, adtMessage, storedFrom, messageDemographics);
        return !existingDemographic.equals(messageDemographics) && existingDemographic.getValidFrom().isBefore(messageDemographics.getValidFrom());
    }

    /**
     * Update core demographics fields from known values of the adt message.
     * @param mrnId           Id of the mrn
     * @param adtMessage      adt message
     * @param storedFrom      when the message has been read by emap core
     * @param coreDemographic original core demographic object
     */
    private void updateCoreDemographicFields(final long mrnId, final AdtMessage adtMessage, final Instant storedFrom,
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
        adtMessage.getPatientDeathIndicator().assignTo(dead -> coreDemographic.setAlive(!dead));
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
    private Mrn createNewLiveMrn(final String mrnString, final String nhsNumber, final String sourceSystem, final Instant messageDateTime,
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
