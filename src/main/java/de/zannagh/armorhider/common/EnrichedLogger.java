package de.zannagh.armorhider.common;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class EnrichedLogger implements Logger {
    @Override
    public String getName() {
        return "armor-hider-logger";
    }
    
    private static String formatMessage(String message){
        return "[Zannagh's Armor Hider] - " + message;
    }
    
    private final Logger del;
    
    public EnrichedLogger(Logger delegate){
        del = delegate;
    }

    @Override
    public boolean isTraceEnabled() {
        return del.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        del.trace(formatMessage(s));
    }

    @Override
    public void trace(String s, Object o) {
        del.trace(formatMessage(s), o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        del.trace(formatMessage(s), o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        del.trace(formatMessage(s), objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        del.trace(formatMessage(s), throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return del.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        del.trace(marker, formatMessage(s));
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        del.trace(marker, formatMessage(s), o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        del.trace(marker, formatMessage(s), o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        del.trace(marker, formatMessage(s), objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        del.trace(marker, formatMessage(s), throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return del.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        del.debug(formatMessage(s));
    }

    @Override
    public void debug(String s, Object o) {
        del.debug(formatMessage(s), o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        del.debug(formatMessage(s), o, o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        del.debug(formatMessage(s), objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        del.debug(formatMessage(s), throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return del.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        del.debug(marker, formatMessage(s));
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        del.debug(marker, formatMessage(s), o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        del.debug(marker, formatMessage(s), o, o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        del.debug(marker, formatMessage(s), objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        del.debug(marker, formatMessage(s), throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return del.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        del.info(formatMessage(s));
    }

    @Override
    public void info(String s, Object o) {
        del.info(formatMessage(s), o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        del.info(formatMessage(s), o, o1);
    }

    @Override
    public void info(String s, Object... objects) {
        del.info(formatMessage(s), objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        del.info(formatMessage(s), throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return del.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        del.info(marker, formatMessage(s));
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        del.info(marker, formatMessage(s), o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        del.info(marker, formatMessage(s), o, o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        del.info(marker, formatMessage(s), objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        del.info(marker, formatMessage(s), throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return del.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        del.warn(formatMessage(s));
    }

    @Override
    public void warn(String s, Object o) {
        del.warn(formatMessage(s), o);
    }

    @Override
    public void warn(String s, Object... objects) {
        del.warn(formatMessage(s), objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        del.warn(formatMessage(s), o, o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        del.warn(formatMessage(s), throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return del.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        del.warn(marker, formatMessage(s));
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        del.warn(marker, formatMessage(s), o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        del.warn(marker, formatMessage(s), o, o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        del.warn(marker, formatMessage(s), objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        del.warn(marker, formatMessage(s), throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return del.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        del.error(formatMessage(s));
    }

    @Override
    public void error(String s, Object o) {
        del.error(formatMessage(s), o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        del.error(formatMessage(s), o, o1);
    }

    @Override
    public void error(String s, Object... objects) {
        del.error(formatMessage(s), objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        del.error(formatMessage(s), throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return del.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        del.error(marker, formatMessage(s));
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        del.error(marker, formatMessage(s), o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        del.error(marker, formatMessage(s), o, o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        del.error(marker, formatMessage(s), objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        del.error(marker, formatMessage(s), throwable);
    }
}
