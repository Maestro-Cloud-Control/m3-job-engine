package io.maestro3.job.engine.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class Assert {

    private static final String NOT_POSITIVE_INTEGER_ERROR_MESSAGE = "%s must be a positive integer, actual value is '%d'";
    private static final String OUT_OF_RANGE_ERROR_MESSAGE = "%s must be in range [%d, %d], actual value is '%d'";

    private Assert() {
        throw new UnsupportedOperationException("Class is not designed for an instantiation");
    }

    public static void notNull(final Object value, final String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void exactlyOneSpecified(final String message, final Object... values) {
        final long specifiedObjectsCount = Optional.ofNullable(values).stream()
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .count();
        if (specifiedObjectsCount != 1) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void positiveInt(final int value, final String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(String.format(NOT_POSITIVE_INTEGER_ERROR_MESSAGE, name, value));
        }
    }

    public static void inRange(final long value, final long from, final long to, final String name) {
        if (value < from || value > to) {
            throw new IllegalArgumentException(String.format(OUT_OF_RANGE_ERROR_MESSAGE, name, from, to, value));
        }
    }
}
