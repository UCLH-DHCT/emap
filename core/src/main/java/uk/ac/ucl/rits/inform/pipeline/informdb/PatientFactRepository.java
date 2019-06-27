package uk.ac.ucl.rits.inform.pipeline.informdb;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.PatientFact;

/**
 * A Repository to handle PatientFacts.
 *
 * @author Jeremy Stein
 */
public interface PatientFactRepository extends CrudRepository<PatientFact, Integer> {
}
