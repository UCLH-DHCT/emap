package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecisionType;

import java.util.Optional;

/**
 * Provides functionality to search advanced decision type table.
 * @author Anika Cawthorn
 */
public interface AdvanceDecisionTypeRepository extends CrudRepository<AdvanceDecisionType, Long> {
    /**
     * Searches for a specific advance decision type based on care code provided.
     * @param advanceDecisionCareCode    Code of advance decision type recorded for an advance decision.
     * @return AdvanceDecisionType for code provided
     */
    Optional<AdvanceDecisionType> findByCareCode(String advanceDecisionCareCode);
}
