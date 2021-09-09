package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecisionType;

import java.util.Optional;

/**
 * Provides functionality to search advanced decision type table.
 * @author Anika Cawthorn
 */
public interface AdvancedDecisionTypeRepository extends CrudRepository<AdvancedDecisionType, Long> {
    /**
     * Searches for a specific advanced decision type based on care code provided.
     * @param advancedDecisionCareCode    Code of advanced decision type recorded for an advanced decision.
     * @return AdvancedDecisionType for code provided
     */
    Optional<AdvancedDecisionType> findByCareCode(String advancedDecisionCareCode);
}
