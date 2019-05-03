package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * This represents a grouper for a visit. A visit can either be either a
 * hospital visit, or just visit to a bed. Visits themselves form a hierarchy
 * where a single hospital visit may contain several bed visits.
 *
 * @author UCL RITS
 *
 */
@Entity
public class VisitFact extends TemporalCore implements FactToProperty<VisitProperty> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long             visitId;

    @ManyToOne
    @JoinColumn(name = "encounter", referencedColumnName = "encounter")
    private Encounter           encounter;

    @ManyToOne
    @JoinColumn(name = "attributeId")
    private Attribute           visitType;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "visit")
    private List<VisitProperty> visitProperties;

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

    /**
     * @return the visitType
     */
    public Attribute getVisitType() {
        return visitType;
    }

    /**
     * @param visitType the visitType to set
     */
    public void setVisitType(Attribute visitType) {
        this.visitType = visitType;
    }

    /**
     * @return the visitProperties for this fact
     */
    public List<VisitProperty> getFactProperties() {
        return visitProperties;
    }

    /**
     * @param visitProperties the visitProperties to set
     */
    public void setVisitProperties(List<VisitProperty> visitProperties) {
        this.visitProperties = visitProperties;
    }

    /**
     * Add a property to a visit.
     *
     * @param prop A single property to append.
     */
    public void addProperty(VisitProperty prop) {
        if (this.visitProperties == null) {
            this.visitProperties = new ArrayList<>();
        }
        this.visitProperties.add(prop);
        prop.setVisit(this);
    }

    @Override
    public void invalidateAll(Instant invalidationDate) {
        setValidUntil(invalidationDate);
        for (VisitProperty vp: getFactProperties()) {
            vp.setValidUntil(invalidationDate);
        }
    }


}
