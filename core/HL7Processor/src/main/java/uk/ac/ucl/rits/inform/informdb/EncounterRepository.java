package uk.ac.ucl.rits.inform.informdb;

import org.springframework.data.repository.CrudRepository;

/**
 */
public interface EncounterRepository extends CrudRepository<Encounter, Integer> {
    /**
     * @param encounter the encounter string
     * @return the Encounter object
     */
    Encounter findEncounterByEncounter(String encounter);
}
