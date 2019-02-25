package uk.ac.ucl.rits.inform.ids;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.repository.CrudRepository;

public interface IdsMasterRepository extends CrudRepository<IdsMaster, Integer> {

    
}