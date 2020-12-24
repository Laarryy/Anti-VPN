package me.egg82.antivpn.utils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {
    private TimeUtil() { }

    private static final Pattern timePattern = Pattern.compile("^(\\d+)\\s*(?:seconds?|s|minutes?|m|hours?|h|days?|d)$");
    private static final Pattern unitPattern = Pattern.compile("^(?:\\d+)\\s*(seconds?|s|minutes?|m|hours?|h|days?|d)$");

    public static Time getTime(String input) {
        Matcher timeMatcher = timePattern.matcher(input);
        if (!timeMatcher.matches()) {
            return null;
        }

        Matcher unitMatcher = unitPattern.matcher(input);
        if (!unitMatcher.matches()) {
            return null;
        }

        long time = Long.parseLong(timeMatcher.group(1));

        char unit = unitMatcher.group(1).charAt(0);
        switch (unit) {
            case 's':
                return new Time(time, TimeUnit.SECONDS);
            case 'm':
                return new Time(time, TimeUnit.MINUTES);
            case 'h':
                return new Time(time, TimeUnit.HOURS);
            case 'd':
                return new Time(time, TimeUnit.DAYS);
            default:
                return null;
        }
    }

    public static String getTimeString(Time time) {
        if (time.unit == TimeUnit.DAYS) {
            return time.time + (time.time == 1 ? "day" : "days");
        } else if (time.unit == TimeUnit.HOURS) {
            return time.time + (time.time == 1 ? "hour" : "hours");
        } else if (time.unit == TimeUnit.MINUTES) {
            return time.time + (time.time == 1 ? "minute" : "minutes");
        } else if (time.unit == TimeUnit.SECONDS) {
            return time.time + (time.time == 1 ? "second" : "seconds");
        }
        return null;
    }

    public static class Time {
        private final long time;
        private final TimeUnit unit;

        private final int hc;

        public Time(long time, TimeUnit unit) {
            if (unit == null) {
                throw new IllegalArgumentException("unit cannot be null.");
            }

            this.time = time;
            this.unit = unit;

            this.hc = Objects.hash(time, unit);
        }

        public long getTime() { return time; }

        public TimeUnit getUnit() { return unit; }

        public long getMillis() { return unit.toMillis(time); }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Time)) return false;
            Time time1 = (Time) o;
            return time == time1.time &&
                    unit == time1.unit;
        }

        public int hashCode() { return hc; }
    }
}
