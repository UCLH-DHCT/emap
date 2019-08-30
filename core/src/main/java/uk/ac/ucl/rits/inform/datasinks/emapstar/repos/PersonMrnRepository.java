package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.PersonMrn;

/**
 * Repository to handle PersonMrn relationships.
 *
 * @author Jeremy Stein
 */
public interface PersonMrnRepository extends CrudRepository<PersonMrn, Integer> {
}
