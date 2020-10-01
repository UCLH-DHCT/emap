package uk.ac.ucl.rits.inform.datasinks.emapstar;

import uk.ac.ucl.rits.inform.interchange.Hl7Value;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;

import java.time.Instant;
import java.util.function.Consumer;

public class RowState<T> {
    private T entity;
    private final boolean entityCreated;
    private final Instant messageDateTime;
    private final Instant storedFrom;
    private boolean entityUpdated = false;

    public RowState(T entity, Instant messageDateTime, Instant storedFrom, boolean entityCreated) {
        this.entity = entity;
        this.messageDateTime = messageDateTime;
        this.storedFrom = storedFrom;
        this.entityCreated = entityCreated;
    }

    public boolean isEntityCreated() {
        return entityCreated;
    }

    public boolean isEntityUpdated() {
        return entityUpdated;
    }

    public T getEntity() {
        return entity;
    }

    public Instant getMessageDateTime() {
        return messageDateTime;
    }

    public Instant getStoredFrom() {
        return storedFrom;
    }


    public void assignHl7ValueIfDifferent(Hl7Value<PatientClass> newValue, String currentValue, Consumer<String> setPatientClass) {
        assignIfDifferent(newValue.toString(), currentValue, setPatientClass);
    }

    public <R> void assignHl7ValueIfDifferent(Hl7Value<R> newValue, R currentValue, Consumer<R> setter) {
        if (newValue.isUnknown()) {
            return;
        }
        assignIfDifferent(newValue.get(), currentValue, setter);
    }

    public <R> void assignIfDifferent(R newValue, R currentValue, Consumer<R> setter) {
        if (!newValue.equals(currentValue)) {
            entityUpdated = true;
            setter.accept(newValue);
        }
    }


}
