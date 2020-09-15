package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

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
     * Merge two MRNs, setting the surviving mrn to be live.
     * @param retiringMrn     MRN to retire and merge from
     * @param survivingMrn    live MRN to merge into
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     */
    public void mergeMrns(final String retiringMrn, final Mrn survivingMrn, final Instant messageDateTime, final Instant storedFrom) {
        // get original mrn object
        Mrn originalMrn = mrnRepo
                .getByMrnEqualsOrMrnIsNullAndNhsNumberEquals(retiringMrn, null)
                .orElseGet(() -> createNewLiveMrn(retiringMrn, null, "EPIC", messageDateTime, storedFrom));
        // change all live mrns from original mrn to surviving mrn
        List<MrnToLive> mrnToLiveRows = mrnToLiveRepo.getAllByLiveMrnIdEquals(originalMrn);
        for (MrnToLive mrnToLive : mrnToLiveRows) {
            mrnToLive.setLiveMrnId(survivingMrn);
            mrnToLiveRepo.save(mrnToLive);
        }
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
    public Mrn getOrCreateMrn(final String mrnString, final String nhsNumber, final String sourceSystem, final Instant messageDateTime,
                              final Instant storedFrom) {
        return mrnRepo
                .getByMrnEqualsOrMrnIsNullAndNhsNumberEquals(mrnString, nhsNumber)
                // mrn exists, get the live mrn
                .map(mrn1 -> mrnToLiveRepo.getByMrnIdEquals(mrn1).getLiveMrnId())
                // otherwise create new mrn and mrn_to_live row
                .orElseGet(() -> createNewLiveMrn(mrnString, nhsNumber, sourceSystem, messageDateTime, storedFrom));
    }

    public void updateOrCreateDemographic(final long mrnId, final AdtMessage adtMessage, final Instant storedFrom) {
        CoreDemographic messageDemographics = new CoreDemographic();
        updateCoreDemographicFields(mrnId, adtMessage, storedFrom, messageDemographics);
        // get existing demographics and update if they have changed. If none existing, save message demographics
        CoreDemographic demographicsToSave = coreDemographicRepo
                .getByMrnIdEquals(mrnId)
                .map(existingDemographic -> {
                    if (!existingDemographic.equals(messageDemographics)) {
                        // log current state to audit table and then update current row
                        updateCoreDemographicFields(mrnId, adtMessage, storedFrom, existingDemographic);
                    }
                    return existingDemographic;
                })
                .orElse(messageDemographics);
        coreDemographicRepo.save(demographicsToSave);
    }

    private CoreDemographic updateCoreDemographicFields(final long mrnId, final AdtMessage adtMessage, final Instant storedFrom,
                                                        CoreDemographic coreDemographic) {
        coreDemographic.setMrnId(mrnId);
        coreDemographic.setFirstname(adtMessage.getPatientGivenName());
        coreDemographic.setMiddlename(adtMessage.getPatientMiddleName());
        coreDemographic.setLastname(adtMessage.getPatientFamilyName());
        coreDemographic.setDateOfBirth(convertToLocalDate(adtMessage.getPatientBirthDate()));
        coreDemographic.setDatetimeOfBirth(adtMessage.getPatientBirthDate());
        coreDemographic.setSex(adtMessage.getPatientSex());
        coreDemographic.setHomePostcode(adtMessage.getPatientZipOrPostalCode());
        // death
        coreDemographic.setAlive(!adtMessage.getPatientDeathIndicator());
        coreDemographic.setDateOfDeath(convertToLocalDate(adtMessage.getPatientDeathDateTime()));
        coreDemographic.setDatetimeOfDeath(adtMessage.getPatientDeathDateTime());
        // from dates
        coreDemographic.setStoredFrom(storedFrom);
        coreDemographic.setValidFrom(adtMessage.getRecordedDateTime());
        return coreDemographic;
    }

    private LocalDate convertToLocalDate(Instant instant) {
        return (instant == null) ? null : instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

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
