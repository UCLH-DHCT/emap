package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.springframework.lang.NonNull;

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
    private long       attributeId;

    @NonNull
    private String     shortName;
    @NonNull
    private String     description;
    @NonNull
    private ResultType resultType;
    @NonNull
    private Instant    addedTime;

    @Transient
    private AttributeKeyMap attributeKey;

}
