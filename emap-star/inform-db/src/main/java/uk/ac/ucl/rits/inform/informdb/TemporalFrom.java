package uk.ac.ucl.rits.inform.informdb;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * Helper class for storing temporal data, as valid from and stored from are almost always used together.
 * @author Stef Piatek
 */
@Data
@AllArgsConstructor
public class TemporalFrom {
    /**
     * Time that the message is valid from.
     */
    private Instant valid;
    /**
     * Time that emap core started processing the message.
     */
    private Instant stored;
}
