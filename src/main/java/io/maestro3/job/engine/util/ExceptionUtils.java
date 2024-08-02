package io.maestro3.job.engine.util;

import java.util.Date;

public final class ExceptionUtils {

    private ExceptionUtils() {
        throw new UnsupportedOperationException("Instantiation is forbidden.");
    }

    public static String exceptionToString(Throwable e) {
        return String.format("Date: %s, Message: %s, StackTrace: [%s]", new Date(),
                org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e),
                org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
    }
}
