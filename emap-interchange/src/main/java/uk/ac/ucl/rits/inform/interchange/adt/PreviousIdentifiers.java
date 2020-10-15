package uk.ac.ucl.rits.inform.interchange.adt;

public interface PreviousIdentifiers {
    /**
     * @return previous MRN string.
     */
    String getPreviousMrn();

    /**
     * @return previous NHS number.
     */
    String getPreviousNhsNumber();

    /**
     * @param previousMrn previous MRN string.
     */
    void setPreviousMrn(String previousMrn);

    /**
     * @param previousNhsNumber previous NHS number.
     */
    void setPreviousNhsNumber(String previousNhsNumber);
}
