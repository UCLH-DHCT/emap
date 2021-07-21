package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;


@SuppressWarnings("serial")
@Entity
@Data
@ToString(callSuper = true)
@Table
public class Department implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long departmentId;

    @ManyToOne
    @JoinColumn(name = "locationId", nullable = false)
    private Location locationId;

    private String hl7String;
    private String name;
    private String speciality;
}
