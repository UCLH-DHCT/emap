package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.Encounter;

/**
 */
public interface EncounterRepository extends CrudRepository<Encounter, Integer> {
    /**
     * @param encounter the encounter string
     * @return the Encounter object
     */
    Encounter findEncounterByEncounter(String encounter);
}
