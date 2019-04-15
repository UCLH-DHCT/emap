package uk.ac.ucl.rits.inform.informdb;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface EncounterRepository extends CrudRepository<Encounter, Integer> {
    public Encounter findEncounterByEncounter(String encounter);
}
