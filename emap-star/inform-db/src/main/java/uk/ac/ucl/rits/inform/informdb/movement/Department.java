package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

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
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
@Table
public class Department extends TemporalCore<Department, DepartmentAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long departmentId;

    @ManyToOne
    @JoinColumn(name = "locationId", nullable = false)
    private Location locationId;

    private String internalId;

    private String name;

    private String speciality;

    private String state;

    public Department() {}

    public Department(Department other) {
        super(other);
        departmentId = other.departmentId;
        locationId = other.locationId;
        internalId = other.internalId;
        name = other.name;
        speciality = other.speciality;
        state = other.state;
    }

    @Override
    public Department copy() {
        return new Department(this);
    }

    @Override
    public DepartmentAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new DepartmentAudit(this, validUntil, storedUntil);
    }
}
