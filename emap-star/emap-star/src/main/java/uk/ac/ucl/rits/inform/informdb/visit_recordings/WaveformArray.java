package uk.ac.ucl.rits.inform.informdb.visit_recordings;

import org.hibernate.HibernateError;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;


public class WaveformArray
        extends AbstractSingleColumnStandardBasicType<Double[]>
        implements DiscriminatorType<Double[]> {

    public WaveformArray(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<Double[]> javaTypeDescriptor) {
        super(sqlTypeDescriptor, javaTypeDescriptor);
    }

    @Override
    public String getName() {
        throw new HibernateError("JES JES JES");
    }

    @Override
    public Double[] stringToObject(String s) throws Exception {
        throw new HibernateError("JES JES JES");
    }

    @Override
    public String objectToSQLString(Double[] doubles, Dialect dialect) throws Exception {
        throw new HibernateError("JES JES JES");
    }
}
