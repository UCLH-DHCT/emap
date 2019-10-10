package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.Person;

/**
 * A repository to handle Persons.
 *
 * @author Jeremy Stein
 */
public interface PersonRepository extends CrudRepository<Person, Integer> {
}
