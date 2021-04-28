package uk.ac.ucl.rits.inform.datasources.ids;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 *
 */
public interface IdsProgressRepository extends CrudRepository<IdsProgress, Integer> {
    /**
     * There is only one row as there is only one number to represent.
     * @return the only row
     */
    @Query("select p from IdsProgress p where p.id=0")
    IdsProgress findOnlyRow();
}
