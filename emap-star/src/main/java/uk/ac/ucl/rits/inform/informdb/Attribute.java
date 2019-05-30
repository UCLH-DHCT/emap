package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.sun.istack.NotNull;

/**
 * An attribute represents a vocabulary item. This may be a question, or answer
 * to a question.
 *
 * @author UCL RITS
 *
 */
@Entity
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long            attributeId;

    @Column(unique = true, nullable = false, length = 15)
    private String          shortName;
    @NotNull
    private String          description;
    @NotNull
    private ResultType      resultType;
    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant         addedTime;

    @Transient
    private AttributeKeyMap attributeKey;

    /**
     * @return the attributeId
     */
    public long getAttributeId() {
        return attributeId;
    }

    /**
     * @param attributeId the attributeId to set
     */
    public void setAttributeId(long attributeId) {
        this.attributeId = attributeId;
    }

    /**
     * @return the shortName
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * @param shortName the shortName to set
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the resultType
     */
    public ResultType getResultType() {
        return resultType;
    }

    /**
     * @param resultType the resultType to set
     */
    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    /**
     * @return the addedTime
     */
    public Instant getAddedTime() {
        return addedTime;
    }

    /**
     * @param addedTime the addedTime to set
     */
    public void setAddedTime(Instant addedTime) {
        this.addedTime = addedTime;
    }

}
