package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
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
@NoArgsConstructor
public class Room implements Serializable {

    /**
     * \brief Unique identifier in EMAP for this room record.
     *
     * This is the primary key for the Room table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long roomId;

    @ManyToOne
    @JoinColumn(name = "departmentId", nullable = false)
    private Department departmentId;

    @Column(nullable = false)
    private String hl7String;
    private String name;

    /**
     * Create new Room.
     * @param hl7String    hl7 string for room
     * @param name         name for room
     * @param departmentId parent Department
     */
    public Room(String hl7String, String name, Department departmentId) {
        this.hl7String = hl7String;
        this.name = name;
        this.departmentId = departmentId;
    }
}
