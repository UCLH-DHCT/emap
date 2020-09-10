package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.time.Instant;
import java.util.Optional;

@Component
public class PersonRepository {
    @Autowired
    MrnRepository mrnRepo;
    @Autowired
    MrnToLiveRepository mrnToLiveRepo;
    @Autowired
    CoreDemographicRepository coreDemographicRepo;

    public Mrn mergeMrns(String originalIdentifier, Mrn survivingMrn, Instant mergeTime) {
        // get original mrn by mrn or nhsNumber
        Optional<Mrn> originalMrn = mrnRepo.getByMrnEqualsOrMrnIsNullAndNhsNumberEquals(originalIdentifier, originalIdentifier);
        //TODO
        //
        return survivingMrn;
    }

    public Mrn getOrCreateMrn(String mrnString, String nhsNumber, String sourceSystem, Instant messageDateTime, Instant storedFrom) {
        // get existing mrn by mrn or (mrn is null and nhsnumber equals)
        Optional<Mrn> optionalMrn = mrnRepo.getByMrnEqualsOrMrnIsNullAndNhsNumberEquals(mrnString, nhsNumber);
        Mrn mrn;
        if (optionalMrn.isPresent()) {
            // mrn exists, get the live mrn
            Mrn messageMrn = optionalMrn.get();
            mrn = mrnToLiveRepo.getByMrnIdEquals(messageMrn).getLiveMrnId();
        } else {
            // create new mrn and mrn_to_live row
            mrn = createNewLiveMrn(mrnString, nhsNumber, sourceSystem, messageDateTime, storedFrom);
        }
        return mrn;
    }

    public CoreDemographic updateOrCreateDemographics(long mrnId, AdtMessage adtMessage, Instant storedFrom) {
        // TODO
        // create demographics from the adtMessage
        CoreDemographic coreDemographic = new CoreDemographic();
        // get current demographics by mrnId
        // if the demographics are not the same, update the demographics
        return coreDemographic;
    }

    private Mrn createNewLiveMrn(String mrnString, String nhsNumber, String sourceSystem, Instant messageDateTime, Instant storedFrom) {
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
