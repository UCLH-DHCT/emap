package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.time.Instant;
import java.util.Optional;

@Component
public class PersonRepository {
    MrnRepository mrnRepo;
    MrnToLiveRepository mrnToLiveRepo;
    CoreDemographicRepository coreDemographicRepo;

    public Mrn mergeMrns(String originalIdentifier, Mrn survivingMrn, Instant mergeTime) {
        // get original mrn by mrn or nhsNumber
        Mrn originalMrn = mrnRepo.getByMrnEqualsOrMrnIsNullAndNhsNumberEquals(originalIdentifier).get();
        //TODO
        //
        return survivingMrn;
    }

    public Mrn getOrCreateMrn(String mrnString, String nhsNumber, String sourceSystem, Instant storedFrom) {
        // TODO
        // get existing mrn by mrn or (mrn is null and nhsnumber equals)
        Optional<Mrn> optionalMrn = mrnRepo.getMrnByMrnEquals(mrnString);
        if (!optionalMrn.isPresent()) {
            optionalMrn = mrnRepo.getByMrnIsNullAndNhsNumberEquals(nhsNumber);
        }
        Mrn mrn;
        if (optionalMrn.isPresent()) {
            mrn = optionalMrn.get();
        } else {
            // otherwise create a new mrn
            // create new mrn to live row with mrn id on both lhs and rhs
            mrn = new Mrn();
            mrn.setMrn(mrnString);
            mrn.setNhsNumber(nhsNumber);
            mrn.setSourceSystem(sourceSystem);
            mrn.setStoredFrom(storedFrom);
            mrnRepo.save(mrn);
            MrnToLive mrnToLive = new MrnToLive();
            mrnToLive.setMrnId(mrn);
            mrnToLive.setLiveMrnId(mrn);
        }

        // look up mrn in lhs id of mrn to live and return the mrn with the rhs id from the function

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

}
