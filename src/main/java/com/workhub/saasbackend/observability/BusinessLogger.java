package com.workhub.saasbackend.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BusinessLogger {

    private static final Logger log = LoggerFactory.getLogger("com.workhub.business");

    private BusinessLogger() {
    }

    public static void info(String event, String message, Object... keyValues) {
        log.info("{} | {} | {}", event, message, formatKeyValues(keyValues));
    }

    public static void warn(String event, String message, Object... keyValues) {
        log.warn("{} | {} | {}", event, message, formatKeyValues(keyValues));
    }

    public static void error(String event, String message, Throwable cause, Object... keyValues) {
        log.error("{} | {} | {}", event, message, formatKeyValues(keyValues), cause);
    }

    private static String formatKeyValues(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                builder.append(' ');
            }
            String key = String.valueOf(keyValues[i]);
            String value = i + 1 < keyValues.length ? String.valueOf(keyValues[i + 1]) : "";
            builder.append(key).append('=').append(value);
        }
        return builder.toString();
    }
}
