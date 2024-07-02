package uk.ac.ucl.rits.inform.informdb.visit_recordings;

import org.hibernate.HibernateError;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

public class WaveformArrayTypeDescriptor extends AbstractTypeDescriptor<Double[]> {

    private static final String DELIMITER = ",";

//    public static final WaveformArrayTypeDescriptor INSTANCE = new WaveformArrayTypeDescriptor();

    protected WaveformArrayTypeDescriptor() {
        super(Double[].class);
    }

    @Override
    public Double[] fromString(String s) {
        throw new HibernateError("JES JES JES");
    }

    @Override
    public <X> X unwrap(Double[] doubles, Class<X> aClass, WrapperOptions wrapperOptions) {
        throw new HibernateError("JES JES JES");
    }

    @Override
    public <X> Double[] wrap(X x, WrapperOptions wrapperOptions) {
        throw new HibernateError("JES JES JES");
    }
}
