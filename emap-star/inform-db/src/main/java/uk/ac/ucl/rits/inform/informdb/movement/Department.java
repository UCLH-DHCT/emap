package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


@SuppressWarnings("serial")
@Entity
@Data
@Table
@NoArgsConstructor
public class Department implements Serializable {

    /**
     * \brief Unique identifier in EMAP for this department record.
     *
     * This is the primary key for the department table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long departmentId;

    /**
     * \brief Text name used by HL7 for this department.
     */
    @Column(nullable = false)
    private String hl7String;

    /**
     * \brief Name of this department.
     */
    private String name;

    /**
     * \brief Speciality of this department.
     */
    private String speciality;

    /**
     * Create department.
     * @param hl7String  hl7 string
     * @param name       name of department
     * @param speciality name of speciality
     */
    public Department(String hl7String, String name, String speciality) {
        this.hl7String = hl7String;
        this.name = name;
        this.speciality = speciality;
    }
}
