package io.maestro3.job.engine;

import io.maestro3.job.engine.tracker.IJobTrackerManager;
import io.maestro3.job.engine.util.Assert;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public class JobManagerConfiguration<O> {

    private final int maxConcurrentTasks;
    private final int postponeCount;
    private final int postponeMinutes;
    private final int jobsLifeTimeDays;
    private final boolean usePrioritySort;
    private final ExecutorService executor;
    private final ThreadFactory threadFactory;
    private final IJobExecutionApprover executionApprover;
    private final IJobTrackerManager<O> trackerManager;

    private JobManagerConfiguration(Builder<O> builder) {
        this.maxConcurrentTasks = builder.maxConcurrentTasks;
        this.postponeCount = builder.postponeCount;
        this.postponeMinutes = builder.postponeMinutes;
        this.jobsLifeTimeDays = builder.jobsLifeTimeDays;
        this.usePrioritySort = builder.usePrioritySort;
        this.executor = builder.executor;
        this.threadFactory = builder.threadFactory;
        this.executionApprover = builder.executionApprover;
        this.trackerManager = builder.trackerManager;
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public int getPostponeCount() {
        return postponeCount;
    }

    public int getPostponeMinutes() {
        return postponeMinutes;
    }

    public int getJobsLifeTimeDays() {
        return jobsLifeTimeDays;
    }

    public boolean usePrioritySort() {
        return usePrioritySort;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public IJobExecutionApprover getExecutionApprover() {
        return executionApprover;
    }

    public IJobTrackerManager<O> getTrackerManager() {
        return trackerManager;
    }

    public static final class Builder<P> {
        private int maxConcurrentTasks;
        private int postponeCount;
        private int postponeMinutes;
        private int jobsLifeTimeDays;
        private boolean usePrioritySort;
        private ExecutorService executor;
        private ThreadFactory threadFactory;
        private IJobExecutionApprover executionApprover;
        private IJobTrackerManager<P> trackerManager;

        public Builder<P> withMaxConcurrentTasks(int maxConcurrentTasks) {
            this.maxConcurrentTasks = maxConcurrentTasks;
            return this;
        }

        public Builder<P> withPostponeCount(int postponeCount) {
            this.postponeCount = postponeCount;
            return this;
        }

        public Builder<P> withPostponeMinutes(int postponeMinutes) {
            this.postponeMinutes = postponeMinutes;
            return this;
        }

        public Builder<P> withJobsLifeTimeDays(int jobsLifeTimeDays) {
            this.jobsLifeTimeDays = jobsLifeTimeDays;
            return this;
        }

        public Builder<P> withPrioritySort(boolean usePrioritySort) {
            this.usePrioritySort = usePrioritySort;
            return this;
        }

        public Builder<P> withExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder<P> withThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public Builder<P> withExecutionApprover(IJobExecutionApprover executionApprover) {
            this.executionApprover = executionApprover;
            return this;
        }

        public Builder<P> withTrackerManager(IJobTrackerManager<P> trackerManager) {
            this.trackerManager = trackerManager;
            return this;
        }

        public JobManagerConfiguration<P> build() {
            Assert.positiveInt(maxConcurrentTasks, "maxConcurrentTasks");
            Assert.positiveInt(postponeCount, "postponeCount");
            Assert.positiveInt(postponeMinutes, "postponeMinutes");
            Assert.positiveInt(jobsLifeTimeDays, "jobsLifeTimeDays");
            Assert.exactlyOneSpecified("exactly one must be specified: executor or thread factory", executor, threadFactory);

            return new JobManagerConfiguration<>(this);
        }
    }
}
