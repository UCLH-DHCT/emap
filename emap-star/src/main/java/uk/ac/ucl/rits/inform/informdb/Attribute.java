package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * An attribute represents a vocabulary item. This may be a question, or answer
 * to a question.
 *
 * @author UCL RITS
 *
 */
@Entity
@Table(indexes = {
        @Index(name = "attribute_valid_from_index", columnList = "validFrom", unique = false),
        @Index(name = "attribute_valid_until_index", columnList = "validUntil", unique = false),
})

public class Attribute implements Serializable {

    private static final long serialVersionUID = -3151350347466393547L;

    @Id
    private Long            attributeId;

    @Column(unique = true, nullable = false, length = 15)
    private String          shortName;
    @Column(nullable = false)
    private String          description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ResultType      resultType;
    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant         validFrom;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant         validUntil;

    @Transient
    private AttributeKeyMap attributeKey;

    /**
     * @return the attributeId
     */
    public Long getAttributeId() {
        return attributeId;
    }

    /**
     * @param attributeId the attributeId to set
     */
    public void setAttributeId(Long attributeId) {
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
     * @return the validFrom - when this attribute was added
     */
    public Instant getValidFrom() {
        return validFrom;
    }

    /**
     * @param validFrom the validFrom time to set
     */
    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    /**
     * @return the validUntil - when this attribute was deprecated
     */
    public Instant getValidUntil() {
        return validUntil;
    }

    /**
     * @param validUntil the validUntil to set
     */
    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    @Override
    public String toString() {
        return shortName + "[" + attributeId + "]";
    }

    @Override
    public int hashCode() {
        // The short name is  what you  are most likely to have
        return this.shortName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // The short name is  what you  are most likely to have
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Attribute other = (Attribute) obj;
        if (!this.shortName.equals(other.shortName)) {
            return false;
        }
        return true;
    }


}
