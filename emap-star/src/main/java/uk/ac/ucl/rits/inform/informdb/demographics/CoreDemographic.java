package uk.ac.ucl.rits.inform.informdb.demographics;

import lombok.Data;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
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
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
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
     * Construct with the Mrn.
     * @param mrnId MRN object.
     */
    public CoreDemographic(Mrn mrnId) {
        super();
        setMrnId(mrnId);
    }


    /**
     * Copy constructor.
     * @param other other demographic.
     */
    private CoreDemographic(CoreDemographic other) {
        super(other);
        coreDemographicId = other.coreDemographicId;
    }

    @Override
    public CoreDemographic copy() {
        return new CoreDemographic(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CoreDemographic that = (CoreDemographic) o;
        return super.equals(that);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
