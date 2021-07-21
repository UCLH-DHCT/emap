package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;


@SuppressWarnings("serial")
@Entity
@Data
@Table
@NoArgsConstructor
public class Department implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long departmentId;

    @Column(nullable = false, unique = true)
    private String hl7String;
    private String name;
    private String speciality;

    public Department(String hl7String, String name, String speciality){
        this.hl7String = hl7String;
        this.name = name;
        this.speciality = speciality;
    }
}
