package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This represents a grouper for a visit. A visit can either be either a
 * hospital visit, or just visit to a bed. Visits themselves form a hierarchy
 * where a single hospital visit may contain several bed visits.
 *
 * @author UCL RITS
 *
 */
@Entity
@JsonIgnoreProperties("encounter")
public class VisitFact extends Fact<VisitFact, VisitProperty> implements Serializable {

    private static final long serialVersionUID = 1049579732074975826L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long             visitId;

    @ManyToOne
    @JoinColumn(name = "encounter", referencedColumnName = "encounter")
    private Encounter           encounter;


    /**
     * @return the visitId
     */
    public Long getVisitId() {
        return visitId;
    }

    /**
     * @param visitId the visitId to set
     */
    public void setVisitId(Long visitId) {
        this.visitId = visitId;
    }

    /**
     * @return the encounter
     */
    public Encounter getEncounter() {
        return encounter;
    }

    /**
     * @param encounter the encounter to set
     */
    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    @Override
    public void addProperty(VisitProperty prop) {
        super.addProperty(prop, this);
    }

}
