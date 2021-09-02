package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationType;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecisionType;

import java.util.Optional;

/**
 * Holds information on advanced decision types.
 * @author Anika Cawthorn
 */
public interface AdvancedDecisionTypeRepository extends CrudRepository<AdvancedDecisionType, Long> {
    /**
     * Searches for a specific advanced decision type based on code provided.
     * @param advancedDecisionType    Code of advanced decision type recorded for an advanced decision.
     * @return AdvancedDecisionType for code provided
     */
    Optional<AdvancedDecisionType> findByCode(String advancedDecisionType);
}
