package me.egg82.antivpn.logging;

import me.egg82.antivpn.config.ConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.Marker;

public class GELFLogger implements Logger {
    private final Logger impl;

    public GELFLogger(@NotNull Logger impl) {
        this.impl = impl;
    }

    @Override
    public String getName() {
        return impl.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return impl.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        impl.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        impl.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        impl.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        impl.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.trace(msg, t);
        } else {
            impl.trace(msg);
        }
        if (impl.isTraceEnabled()) {
            GELFLoggerUtil.queue(0, msg, t);
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return impl.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        impl.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        impl.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        impl.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        impl.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.trace(marker, msg, t);
        } else {
            impl.trace(marker, msg);
        }
        if (impl.isTraceEnabled(marker)) {
            GELFLoggerUtil.queue(0, msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return impl.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        impl.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        impl.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        impl.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        impl.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.debug(msg, t);
        } else {
            impl.debug(msg);
        }
        if (impl.isDebugEnabled()) {
            GELFLoggerUtil.queue(0, msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return impl.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        impl.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        impl.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        impl.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        impl.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.debug(marker, msg, t);
        } else {
            impl.debug(marker, msg);
        }
        if (impl.isDebugEnabled(marker)) {
            GELFLoggerUtil.queue(0, msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return impl.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        impl.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        impl.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        impl.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        impl.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.info(msg, t);
        } else {
            impl.info(msg);
        }
        if (impl.isInfoEnabled()) {
            GELFLoggerUtil.queue(1, msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return impl.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        impl.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        impl.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        impl.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        impl.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.info(marker, msg, t);
        } else {
            impl.info(marker, msg);
        }
        if (impl.isInfoEnabled(marker)) {
            GELFLoggerUtil.queue(1, msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return impl.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        impl.warn(msg);
        if (impl.isWarnEnabled()) {
            GELFLoggerUtil.queue(2, msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        impl.warn(format, arg);
        if (impl.isWarnEnabled()) {
            GELFLoggerUtil.queue(2, String.format(format, arg));
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        impl.warn(format, arguments);
        if (impl.isWarnEnabled()) {
            GELFLoggerUtil.queue(2, String.format(format, arguments));
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        impl.warn(format, arg1, arg2);
        if (impl.isWarnEnabled()) {
            GELFLoggerUtil.queue(2, String.format(format, arg1, arg2));
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.warn(msg, t);
        } else {
            impl.warn(msg);
        }
        if (impl.isWarnEnabled()) {
            GELFLoggerUtil.queue(2, msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return impl.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        impl.warn(marker, msg);
        if (impl.isWarnEnabled(marker)) {
            GELFLoggerUtil.queue(2, msg);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        impl.warn(marker, format, arg);
        if (impl.isWarnEnabled(marker)) {
            GELFLoggerUtil.queue(2, String.format(format, arg));
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        impl.warn(marker, format, arg1, arg2);
        if (impl.isWarnEnabled(marker)) {
            GELFLoggerUtil.queue(2, String.format(format, arg1, arg2));
        }
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        impl.warn(marker, format, arguments);
        if (impl.isWarnEnabled(marker)) {
            GELFLoggerUtil.queue(2, String.format(format, arguments));
        }
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.warn(marker, msg, t);
        } else {
            impl.warn(marker, msg);
        }
        if (impl.isWarnEnabled(marker)) {
            GELFLoggerUtil.queue(2, msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return impl.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        impl.error(msg);
        if (impl.isErrorEnabled()) {
            GELFLoggerUtil.queue(3, msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        impl.error(format, arg);
        if (impl.isErrorEnabled()) {
            GELFLoggerUtil.queue(3, String.format(format, arg));
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        impl.error(format, arg1, arg2);
        if (impl.isErrorEnabled()) {
            GELFLoggerUtil.queue(3, String.format(format, arg1, arg2));
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        impl.error(format, arguments);
        if (impl.isErrorEnabled()) {
            GELFLoggerUtil.queue(3, String.format(format, arguments));
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.error(msg, t);
        } else {
            impl.error(msg);
        }
        if (impl.isErrorEnabled()) {
            GELFLoggerUtil.queue(3, msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return impl.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        impl.error(marker, msg);
        if (impl.isErrorEnabled(marker)) {
            GELFLoggerUtil.queue(3, msg);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        impl.error(marker, format, arg);
        if (impl.isErrorEnabled(marker)) {
            GELFLoggerUtil.queue(3, String.format(format, arg));
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        impl.error(marker, format, arg1, arg2);
        if (impl.isErrorEnabled(marker)) {
            GELFLoggerUtil.queue(3, String.format(format, arg1, arg2));
        }
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        impl.error(marker, format, arguments);
        if (impl.isErrorEnabled(marker)) {
            GELFLoggerUtil.queue(3, String.format(format, arguments));
        }
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (ConfigUtil.getDebugOrFalse()) {
            impl.error(marker, msg, t);
        } else {
            impl.error(marker, msg);
        }
        if (impl.isErrorEnabled(marker)) {
            GELFLoggerUtil.queue(3, msg, t);
        }
    }
}
