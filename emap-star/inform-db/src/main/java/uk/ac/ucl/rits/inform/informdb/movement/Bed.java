package uk.ac.ucl.rits.inform.informdb.movement;

import lombok.Data;

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
public class Bed implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bedId;

    @ManyToOne
    @JoinColumn(name = "locationId", nullable = false)
    private Location locationId;

    @ManyToOne
    @JoinColumn(name = "roomId", nullable = false)
    private Room roomId;

    @Column(nullable = false)
    private String hl7String;
}
