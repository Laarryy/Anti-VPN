package me.egg82.antivpn.utils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {
    private TimeUtil() {}

    private static final Pattern timePattern = Pattern.compile("^(\\d+)\\s*(?:seconds?|s|minutes?|m|hours?|h|days?|d)$");
    private static final Pattern unitPattern = Pattern.compile("^(?:\\d+)\\s*(seconds?|s|minutes?|m|hours?|h|days?|d)$");

    public static Optional<Long> getTime(String input) {
        Matcher matcher = timePattern.matcher(input);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(Long.parseLong(matcher.group(1)));
    }

    public static Optional<TimeUnit> getUnit(String input) {
        Matcher matcher = unitPattern.matcher(input);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        char unit = matcher.group(1).charAt(0);
        switch (unit) {
            case 's':
                return Optional.of(TimeUnit.SECONDS);
            case 'm':
                return Optional.of(TimeUnit.MINUTES);
            case 'h':
                return Optional.of(TimeUnit.HOURS);
            case 'd':
                return Optional.of(TimeUnit.DAYS);
            default:
                return Optional.empty();
        }
    }
}
