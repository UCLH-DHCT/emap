package uk.ac.ucl.rits.inform.pipeline.informdb;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.PersonMrn;

/**
 * Repository to handle PersonMrn relationships.
 *
 * @author Jeremy Stein
 */
public interface PersonMrnRepository extends CrudRepository<PersonMrn, Integer> {
}
