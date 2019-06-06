package uk.ac.ucl.rits.inform.informdb;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 */
public interface AttributeRepository extends CrudRepository<Attribute, String> {
    /**
     * @param sn shortname of the attribute to look up
     * @return the Attribute object if it exists
     */
    @Query("select a from Attribute a where a.shortName=?1")
    Optional<Attribute> findByShortName(String sn);
}
