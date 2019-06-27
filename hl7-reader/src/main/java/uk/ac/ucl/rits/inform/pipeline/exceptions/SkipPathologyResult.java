package uk.ac.ucl.rits.inform.pipeline.exceptions;

/**
 * An ignored pathology message.
 *
 * @author Jeremy Stein
 *
 */
public class SkipPathologyResult extends MessageIgnoredException {

    private static final long serialVersionUID = 2923902031304532480L;

    /**
     * Create a new SkipPathologyResult.
     *
     * @param string the message
     */
    public SkipPathologyResult(String string) {
        super(string);
    }
}
