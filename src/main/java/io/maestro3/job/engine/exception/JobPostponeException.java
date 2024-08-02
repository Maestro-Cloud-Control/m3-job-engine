package io.maestro3.job.engine.exception;

import io.maestro3.job.engine.util.Assert;

public class JobPostponeException extends JobExecutionException {
    public static final int MIN_CUSTOM_POSTPONE_MINUTES = 1;
    public static final int MAX_CUSTOM_POSTPONE_MINUTES = 360;
    private final Integer customPostponeMinutes;

    public JobPostponeException(String message) {
        super(message);
        this.customPostponeMinutes = null;
    }

    public JobPostponeException(String message, Throwable e) {
        super(message, e);
        this.customPostponeMinutes = null;
    }

    public JobPostponeException(String message, Integer customPostponeMinutes) {
        super(message);
        Assert.notNull(customPostponeMinutes, "customPostponeMinutes must not be null");
        Assert.inRange(customPostponeMinutes, MIN_CUSTOM_POSTPONE_MINUTES, MAX_CUSTOM_POSTPONE_MINUTES, "customPostponeMinutes");
        this.customPostponeMinutes = customPostponeMinutes;
    }

    public Integer getCustomPostponeMinutes() {
        return customPostponeMinutes;
    }
}
