package me.egg82.antivpn;

public class APIException extends Exception {
    private final boolean hard;

    public APIException(boolean hard, String message) {
        super(message);
        this.hard = hard;
    }

    public APIException(boolean hard, Exception inner) {
        super(inner);
        this.hard = hard;
    }

    public APIException(boolean hard, String message, Exception inner) {
        super(message, inner);
        this.hard = hard;
    }

    /**
     * A boolean representing whether or not the exception is "hard"
     * A "hard" exception means an internal failure or exception in the API, and is usually logged
     * A "soft" (not hard) exception means a user error or otherwise easily-correctable error
     * @return A boolean representing whether or not the exception is "hard"
     */
    public boolean isHard() { return hard; }
}
