package me.egg82.antivpn.api;

import org.jetbrains.annotations.Nullable;

/**
 * An exception thrown from the Anti-VPN API.
 */
public class APIException extends RuntimeException {
    private final boolean hard;

    public APIException(boolean hard, @Nullable String message) {
        super(message);
        this.hard = hard;
    }

    public APIException(boolean hard, @Nullable Throwable cause) {
        super(cause);
        this.hard = hard;
    }

    public APIException(boolean hard, @Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.hard = hard;
    }

    /**
     * A boolean representing whether or not the exception is "hard".
     *
     * <p>A "hard" exception means an internal failure or exception in the plugin or its configuration, and is usually logged.</p>
     * <p>A "soft" (not hard) exception means a user error, temporary API malfunction, or otherwise easily-correctable issue.</p>
     *
     * @return true if the exception is "hard", false if not
     */
    public boolean isHard() {
        return hard;
    }
}
