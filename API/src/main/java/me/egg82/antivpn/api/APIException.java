package me.egg82.antivpn.api;

/**
 * An exception thrown from the AntiVPN API.
 */
public class APIException extends RuntimeException {
    private final boolean hard;

    public APIException(boolean hard, String message) {
        super(message);
        this.hard = hard;
    }

    public APIException(boolean hard, Throwable cause) {
        super(cause);
        this.hard = hard;
    }

    public APIException(boolean hard, String message, Throwable cause) {
        super(message, cause);
        this.hard = hard;
    }

    /**
     * A boolean representing whether or not the exception is "hard".
     *
     * <p>A "hard" exception means an internal failure or exception in the API or its configuration, and is usually logged.</p>
     * <p>A "soft" (not hard) exception means a user error, temporary API malfunction, or otherwise easily-correctable issue.</p>
     *
     * @return true if the exception is "hard", false if not
     */
    public boolean isHard() { return hard; }
}
