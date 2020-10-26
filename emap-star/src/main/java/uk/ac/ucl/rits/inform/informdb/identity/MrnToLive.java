package uk.ac.ucl.rits.inform.informdb.identity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * Over time MRNs are merged into others as more is found out about a patient.
 * This table stores a mapping from every MRN known to the system, to its
 * currently live (in use) MRN.
 * @author UCL RITS
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class MrnToLive extends MrnToLiveParent {

    private static final long serialVersionUID = 8891761742756656453L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long mrnToLiveId;


    public MrnToLive() {}

    public MrnToLive(MrnToLive other) {
        super(other);
        this.setMrnId(other.getMrnId());
        this.setLiveMrnId(other.getLiveMrnId());
    }

    @Override
    public MrnToLive copy() {
        return new MrnToLive(this);
    }

}
