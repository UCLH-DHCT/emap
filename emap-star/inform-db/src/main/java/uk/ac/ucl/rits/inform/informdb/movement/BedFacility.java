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
public class BedFacility implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bedFacilityId;

    @ManyToOne
    @JoinColumn(name = "bedStateId", nullable = false)
    private BedState bedStateId;

    /**
     * Type of facility available at bed.
     * e.g. Cot, oxygen, Near Nurses Station...
     */
    @Column(nullable = false)
    private String type;


    /**
     * Create Bed Facility.
     * @param bedStateId parent bed state
     * @param type       type of facility
     */
    public BedFacility(BedState bedStateId, String type) {
        this.bedStateId = bedStateId;
        this.type = type;
    }
}
