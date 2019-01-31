package uk.ac.ucl.rits.inform.informdb;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface EncounterRepository extends CrudRepository<Encounter, Integer> {

}
