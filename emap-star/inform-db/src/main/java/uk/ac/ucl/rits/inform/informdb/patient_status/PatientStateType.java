package uk.ac.ucl.rits.inform.informdb.patient_status;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
public class PatientStateType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long patientStateTypeId;

    /**
     * problem list or patient infection
     */
    @Column(nullable = false)
    private String type;

    /**
     * disease or infection type
     */
    @Column(nullable = false)
    private String name;
    private String standardisedCode;
    private String standardisedVocabulary;

}
