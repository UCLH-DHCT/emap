package uk.ac.ucl.rits.inform.informdb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.Index;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AuditTable {

    /**
     * Optional indexes to pass to the generated @Table.
     *
     * @return Array of indexes to pass.
     */
    Index[] indexes() default {};
}
