package uk.ac.ucl.rits.inform.informdb;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface VisitFactRepository extends CrudRepository<VisitFact, Integer> {

    @Query("select vf from VisitFact vf "
            + "inner join vf.visitProperties vp "
            + "inner join vf.encounter enc "
            + "inner join enc.mrn mrn "
            + "inner join vp.attribute attr " 
            + "where mrn.mrn=?1 "
            + "and attr.shortName=?2 " 
            + "and vp.valueAsDatetime is not null "
            + "order by vp.valueAsDatetime desc ")
    List<VisitFact> findLatestVisitsByMrn(String mrn, String attrShortName);
}
