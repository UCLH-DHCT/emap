package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.Instant;


@SuppressWarnings("serial")
@Entity
@Data
@ToString(callSuper = true)
@Table
@NoArgsConstructor
public class DepartmentState implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long departmentStateId;

    @ManyToOne
    @JoinColumn(name = "departmentId", nullable = false)
    private Department departmentId;

    private String status;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant validFrom;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant validUntil;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedFrom;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedUntil;

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
        this.validFrom = validFrom;
        this.storedFrom = storedFrom;
    }
}
