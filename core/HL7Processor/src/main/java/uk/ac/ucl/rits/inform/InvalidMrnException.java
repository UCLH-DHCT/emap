package uk.ac.ucl.rits.inform;

public class InvalidMrnException extends RuntimeException {

    public InvalidMrnException(String string) {
        super(string);
    }

    public InvalidMrnException() {
    }

}
