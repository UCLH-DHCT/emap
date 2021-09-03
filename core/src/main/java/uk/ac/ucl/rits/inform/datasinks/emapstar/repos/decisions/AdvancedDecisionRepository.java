package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecision;

import java.util.Optional;

/**
 * Repository to hold advanced decisions registered by patients.
 * @author Anika Cawthorn
 */
public interface AdvancedDecisionRepository extends CrudRepository<AdvancedDecision, Long> {
    /**
     * Find advanced decision by unique identifier.
     * @param advancedDecisionId   Advanced decision identifier.
     * @return possible ConsultationRequest
     */
    Optional<AdvancedDecision> findByAdvancedDecisionNumber(Long advancedDecisionId);
}
