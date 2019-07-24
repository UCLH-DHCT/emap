package uk.ac.ucl.rits.inform.pipeline.informdb;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 */
@EntityScan("uk.ac.ucl.rits.inform.pipeline.informdb")
public interface IdsProgressRepository extends CrudRepository<IdsProgress, Integer> {

    /**
     * There is only one row as there is only one number to represent.
     * @return the only row
     */
    @Query("select p from IdsProgress p where p.id=0")
    IdsProgress findOnlyRow();

}
