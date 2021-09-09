package uk.ac.ucl.rits.inform.informdb.decisions;

import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Advanced decisions can be different in nature and therefore are distinguished by their type. Each of these types is
 * identified by a care code and name, e.g. "DNACPR" with care code "COD4".
 * @author Anika Cawthorn
 */
@Entity
@Data
@NoArgsConstructor
@AuditTable
public class AdvancedDecisionType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long advancedDecisionTypeId;

    /**
     * Name of the advanced decision type, e.g. DNACPR
     */
    @Column(nullable = false)
    private String name;

    /**
     * Code used within EPIC for advanced decision type, e.g COD4 for DNACPR.
     */
    @Column(nullable = false, unique = true)
    private String careCode;

    /**
     * Minimal information constructor.
     * @param name          Name of advanced decision type.
     * @param careCode      Care code relating to advanced decision type.
     */
    public AdvancedDecisionType(String careCode, String name) {
        this.name = name;
        this.careCode = careCode;
    }
}
