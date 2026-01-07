package ch.sbb.polarion.extension.generic.exception;

public class DuplicateSettingNameException extends RuntimeException {
    public DuplicateSettingNameException(String message) {
        super(message);
    }

    public DuplicateSettingNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
