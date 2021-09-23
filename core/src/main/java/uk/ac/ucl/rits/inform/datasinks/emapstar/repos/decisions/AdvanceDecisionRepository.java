package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecision;

import java.util.Optional;

/**
 * Repository to interact with advanced decisions registered by patients.
 * @author Anika Cawthorn
 */
public interface AdvanceDecisionRepository extends CrudRepository<AdvanceDecision, Long> {
    /**
     * Find advanced decision by unique, internal identifier.
     * @param internalId Internal identifier for advanced decision.
     * @return possible AdvancedDecision
     */
    Optional<AdvanceDecision> findByInternalId(Long internalId);
}
