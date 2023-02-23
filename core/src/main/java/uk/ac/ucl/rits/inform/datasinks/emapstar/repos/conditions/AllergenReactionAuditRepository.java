package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.AllergenReactionAudit;

/**
 * AllergenReaction repository.
 * @author Tom Young
 */
public interface AllergenReactionAuditRepository extends CrudRepository<AllergenReactionAudit, Long> {

}
