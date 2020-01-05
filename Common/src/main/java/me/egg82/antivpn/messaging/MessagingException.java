package me.egg82.antivpn.messaging;

public class MessagingException extends Exception {
    private final boolean automaticallyRecoverable;
    public boolean isAutomaticallyRecoverable() { return automaticallyRecoverable; }

    public MessagingException(boolean automaticallyRecoverable, String message) {
        super(message);
        this.automaticallyRecoverable = automaticallyRecoverable;
    }

    public MessagingException(boolean automaticallyRecoverable, Throwable cause) {
        super(cause);
        this.automaticallyRecoverable = automaticallyRecoverable;
    }

    public MessagingException(boolean automaticallyRecoverable, String message, Throwable cause) {
        super(message, cause);
        this.automaticallyRecoverable = automaticallyRecoverable;
    }
}
