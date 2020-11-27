package uk.ac.ucl.rits.inform.informdb.visit_recordings;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * VisitObservationType describes the meaning behind a specific observations. In
 * EHR systems these are often coded either with potentially ambiguous short
 * names, or sometimes just numbers. This table maps these system level terms
 * into standardised vocabularies to make their meanings clear.
 * @author Roma Klapaukh & Stef Piatek
 */
@Data
@SuppressWarnings("serial")
@Entity
@NoArgsConstructor
public class VisitObservationType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long visitObservationType;

    /**
     * The hospital system that emap received the data from.
     */
    @Column(nullable = false)
    private String sourceSystem;

    /**
     * The application that generated the data, this can be the same as the source system.
     */
    @Column(nullable = false)
    private String sourceApplication;

    /**
     * The code used by the hospital application to identify the observation type.
     */
    @Column(nullable = false)
    private String idInApplication;

    /**
     * Readable name for the hospital application observation type.
     */
    private String nameInApplication;

    /**
     * Mapping code for the observation from the standardised vocabulary system.
     */
    private String standardisedCode;

    /**
     * Nomenclature or classification system used.
     */
    private String standardisedVocabulary;

    /**
     * Data type expected to be returned.
     */
    private String primaryDataType;

    /**
     * Minimal information constructor.
     * @param idInApplication   Id from the application
     * @param sourceSystem      source system
     * @param sourceApplication source application
     */
    public VisitObservationType(String idInApplication, String sourceSystem, String sourceApplication) {
        this.idInApplication = idInApplication;
        this.sourceSystem = sourceSystem;
        this.sourceApplication = sourceApplication;
    }
}
