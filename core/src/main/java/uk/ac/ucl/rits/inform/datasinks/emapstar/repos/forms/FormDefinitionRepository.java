package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.forms;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinition;

import java.util.Optional;

public interface FormDefinitionRepository extends CrudRepository<FormDefinition, Long> {
    Optional<FormDefinition> findByInternalId(String formSourceId);
}
