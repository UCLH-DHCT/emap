package uk.ac.ucl.rits.inform.informdb.patient_status;

import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class PatientState {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long patientStateId;

    @ManyToOne
    @JoinColumn(name = "patientStateTypeId", nullable = false)
    private PatientStateType patientStateTypeId;

    @ManyToOne
    @JoinColumn(name = "mnrId", nullable = false)
    private Mrn mrnId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId")
    private HospitalVisit hospitalVisitId;

    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant addedDateTime;

    private LocalDate resolutionDate;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant onsetDate;

    /**
     * temporary infection?
     */
    private String classification;

    private String status;

    private String priority;

    @Column(columnDefinition = "text")
    private String comment;
}

