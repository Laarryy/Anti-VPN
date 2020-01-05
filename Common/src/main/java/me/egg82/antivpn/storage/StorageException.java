package me.egg82.antivpn.storage;

public class StorageException extends Exception {
    private final boolean automaticallyRecoverable;
    public boolean isAutomaticallyRecoverable() { return automaticallyRecoverable; }

    public StorageException(boolean automaticallyRecoverable, String message) {
        super(message);
        this.automaticallyRecoverable = automaticallyRecoverable;
    }

    public StorageException(boolean automaticallyRecoverable, Throwable cause) {
        super(cause);
        this.automaticallyRecoverable = automaticallyRecoverable;
    }

    public StorageException(boolean automaticallyRecoverable, String message, Throwable cause) {
        super(message, cause);
        this.automaticallyRecoverable = automaticallyRecoverable;
    }
}
