package uk.ac.ucl.rits.inform;

/** Something bad and unexpected has been discovered in Inform-db
 * such that we shouldn't continue any further and can't recover. 
 * @author jeremystein
 */
public class InformDbIntegrityException extends RuntimeException {

    public InformDbIntegrityException(String string) {
        super(string);
    }

}
