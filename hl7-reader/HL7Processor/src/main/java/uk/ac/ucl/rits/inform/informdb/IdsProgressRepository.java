package uk.ac.ucl.rits.inform.informdb;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

@EntityScan("uk.ac.ucl.rits.inform.informdb")
public interface IdsProgressRepository extends CrudRepository<IdsProgress, Integer> {

    // currently there is only one row as there is only one number to represent
    // progress
    // (ie. the latest HL7 unid we have processed)
    @Query("select p from IdsProgress p where p.id=0")
    public IdsProgress findOnlyRow();

}
