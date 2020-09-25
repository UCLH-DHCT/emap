package uk.ac.ucl.rits.inform.informdb.demographics;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A core demographic represents the main demographics stored around patients.
 * These are attached to an MRN and describe patient level, rather than visit
 * level, data.
 * @author UCL RITS
 */
@Entity
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class CoreDemographic extends CoreDemographicParent {
    private static final long serialVersionUID = -5997494172438793019L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long coreDemographicId;

    /**
     * Default constructor.
     */
    public CoreDemographic() {
    }


    /**
     * Copy constructor.
     * @param other other demographic.
     */
    public CoreDemographic(CoreDemographic other) {
        super(other);
    }

    @Override
    public CoreDemographic copy() {
        return new CoreDemographic(this);
    }
}
