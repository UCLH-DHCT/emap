package uk.ac.ucl.rits.inform.informdb;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface MrnRepository extends CrudRepository<Mrn, Integer> {
    // according to the schema as it stands, we can have multiple IDs
    @Query("select m from Mrn m where m.mrn=?1")
    public List<Mrn> findByMrnString(String mrn);
}
