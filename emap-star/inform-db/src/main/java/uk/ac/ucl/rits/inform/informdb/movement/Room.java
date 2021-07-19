package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;

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
@Table
@Data
public class Room implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long roomId;

    @ManyToOne
    @JoinColumn(name = "locationId", nullable = false)
    private Location locationId;

    @ManyToOne
    @JoinColumn(name = "departmentId", nullable = false)
    private Department departmentId;

    private String name;

    public Room() {
    }
}
