package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecision;

/**
 * Repository to hold advanced decisions registered by patients.
 * @author Anika Cawthorn
 */
public interface AdvancedDecisionRepository extends CrudRepository<AdvancedDecision, Long> {

}
