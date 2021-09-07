package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.AuditCore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;


@SuppressWarnings("serial")
@Entity
@Data
@ToString(callSuper = true)
@Table
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DepartmentState extends AuditCore<DepartmentState> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long departmentStateId;

    @ManyToOne
    @JoinColumn(name = "departmentId", nullable = false)
    private Department departmentId;

    private String status;

    /**
     * Create valid department state.
     * @param department parent department
     * @param status     status of department
     * @param validFrom  time that the message was valid from
     * @param storedFrom time that emap core stared processing the message
     */
    public DepartmentState(Department department, String status, Instant validFrom, Instant storedFrom) {
        departmentId = department;
        this.status = status;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    private DepartmentState(DepartmentState other) {
        super(other);
        setValidFrom(other.getValidFrom());
        setStoredFrom(other.getValidUntil());
        departmentId = other.departmentId;
        status = other.status;
    }

    @Override
    public DepartmentState copy() {
        return new DepartmentState(this);
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public DepartmentState createAuditEntity(Instant validUntil, Instant storedUntil) {
        DepartmentState audit = copy();
        audit.setValidUntil(validUntil);
        audit.setStoredUntil(storedUntil);
        return audit;
    }
}
