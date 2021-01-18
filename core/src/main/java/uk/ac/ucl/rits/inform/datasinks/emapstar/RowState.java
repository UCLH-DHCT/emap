package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;
import uk.ac.ucl.rits.inform.informdb.AuditCore;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Track the state of a hibernate entity.
 * All values for the entity should be updated from the assign*IfDifferent method of this class to track the state and
 * automatically update the validFrom and storedFrom fields.
 * @param <T> Hibernate Entity type that has validFrom and storedFrom fields.
 * @param <A> The AuditEntity Type
 */
public class RowState<T extends TemporalCore<T, A>, A extends AuditCore> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private T entity;
    private final T originalEntity;
    private final boolean entityCreated;
    private final Instant messageDateTime;
    private final Instant storedFrom;
    private boolean entityUpdated = false;

    /**
     * @param entity          hibernate entity
     * @param messageDateTime date time of the message
     * @param storedFrom      when the message has been read by emap core
     * @param entityCreated   if the entity has been created (instead of already existing in the database)
     */
    public RowState(T entity, Instant messageDateTime, Instant storedFrom, boolean entityCreated) {
        this.entity = entity;
        this.messageDateTime = messageDateTime;
        this.storedFrom = storedFrom;
        this.entityCreated = entityCreated;
        originalEntity = entity.copy();
    }

    /**
     * @return was the entity created by this message.
     */
    public boolean isEntityCreated() {
        return entityCreated;
    }

    /**
     * @return was the entity updated by this message.
     */
    public boolean isEntityUpdated() {
        return entityUpdated;
    }

    /**
     * @return the hibernate entity.
     */
    public T getEntity() {
        return entity;
    }

    /**
     * @param entityUpdated is entity updated
     */
    public void setEntityUpdated(boolean entityUpdated) {
        this.entityUpdated = entityUpdated;
    }

    /**
     * If new value is different assign from HL7Value of PatientClass to a setter taking a string.
     * @param newValue        new value
     * @param currentValue    current value
     * @param setPatientClass setter lambda
     */
    public void assignHl7ValueIfDifferent(Hl7Value<PatientClass> newValue, String currentValue, Consumer<String> setPatientClass) {
        if (newValue.isUnknown()) {
            return;
        }
        assignIfDifferent(newValue.get().toString(), currentValue, setPatientClass);
    }


    /**
     * Assign new Instant value to LocalDate if different.
     * @param newValue        new value
     * @param currentValue    current value
     * @param setPatientClass setter lambda
     */
    public void assignHl7ValueIfDifferent(Hl7Value<Instant> newValue, LocalDate currentValue, Consumer<LocalDate> setPatientClass) {
        if (newValue.isUnknown()) {
            return;
        }
        Instant unpackedValue = newValue.get();
        LocalDate dateTime = (unpackedValue == null) ? null : unpackedValue.atZone(ZoneId.systemDefault()).toLocalDate();
        assignIfDifferent(dateTime, currentValue, setPatientClass);
    }


    /**
     * If new value is different assign from HL7Value to a setter taking the same type.
     * @param newValue     new value
     * @param currentValue current value
     * @param setter       setter lambda
     * @param <R>          type of the value in the hibernate entity
     */
    public <R> void assignHl7ValueIfDifferent(Hl7Value<R> newValue, R currentValue, Consumer<R> setter) {
        if (newValue.isUnknown()) {
            return;
        }
        assignIfDifferent(newValue.get(), currentValue, setter);
    }

    /**
     * Directly assign a new value if it is different from current value.
     * @param newValue     new value
     * @param currentValue current value
     * @param setter       setter lambda
     * @param <R>          type of the value in the hibernate entity
     * @return true if state has been updated
     */
    public <R> boolean assignIfDifferent(@Nullable R newValue, @Nullable R currentValue, Consumer<R> setter) {
        if (Objects.equals(newValue, currentValue)) {
            return false;
        }
        entityUpdated = true;
        entity.setStoredFrom(storedFrom);
        entity.setValidFrom(messageDateTime);
        setter.accept(newValue);
        return true;
    }


    /**
     * If current value exists, remove it and set validFrom cancellation time.
     * @param currentValue      current value
     * @param setter            setter lambda
     * @param cancelledDateTime Time of cancellation
     * @param <R>               type of the value in the hibernate entity
     */
    public <R> void removeIfExists(@Nullable R currentValue, Consumer<R> setter, Instant cancelledDateTime) {
        boolean removed = assignIfDifferent(null, currentValue, setter);
        if (removed && cancelledDateTime != null) {
            entity.setValidFrom(cancelledDateTime);
        }
    }

    /**
     * Save entity if it is created, or auditlog if the entity has been updated.
     * @param entityRepo entity repository
     * @param auditRepo  audit repository
     */
    public void saveEntityOrAuditLogIfRequired(CrudRepository<T, Long> entityRepo, CrudRepository<A, Long> auditRepo) {
        if (entityCreated) {
            logger.info("New Entity saved: {}", entityRepo.save(entity));
        } else if (entityUpdated) {
            A auditEntity = originalEntity.createAuditEntity(messageDateTime, storedFrom);
            auditRepo.save(auditEntity);
            logger.info("New AuditEntity being saved: {}", auditEntity);
        }
    }
}
