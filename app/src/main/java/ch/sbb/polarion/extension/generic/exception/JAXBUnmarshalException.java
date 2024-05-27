package ch.sbb.polarion.extension.generic.exception;

public class JAXBUnmarshalException extends RuntimeException {
    public JAXBUnmarshalException(String message, Throwable linkedException) {
        super(message, linkedException);
    }
}
