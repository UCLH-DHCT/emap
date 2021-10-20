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
public class Bed implements Serializable {

    /**
     * \brief Unique identifier in EMAP for this bed record.
     *
     * This is the primary key for the Bed table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bedId;

    @ManyToOne
    @JoinColumn(name = "roomId", nullable = false)
    private Room roomId;

    @Column(nullable = false)
    private String hl7String;

    /**
     * Create new bed.
     * @param hl7String hl7 string for bed
     * @param roomId room id
     */
    public Bed(String hl7String, Room roomId) {
        this.hl7String = hl7String;
        this.roomId = roomId;
    }
}
