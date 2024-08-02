package io.maestro3.job.engine.tracker.impl;

import io.maestro3.job.engine.model.IJob;
import io.maestro3.job.engine.model.JobStats;
import io.maestro3.job.engine.tracker.IJobTracker;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractJobTracker<O> implements IJobTracker<O> {

    private final String jobType;
    private final int statsGranularityMinutes;
    private final Map<Long, JobStats> stats;

    protected AbstractJobTracker(String jobType,
                                 int statsGranularityMinutes,
                                 int statsStorageMinutes) {
        this.jobType = jobType;
        this.statsGranularityMinutes = statsGranularityMinutes;
        int maxStatsDataPoints = statsStorageMinutes / statsGranularityMinutes;
        this.stats = Collections.synchronizedMap(new JobStatsMap(maxStatsDataPoints));
    }

    @Override
    public String getSupportedJobType() {
        return jobType;
    }

    @Override
    public void onCreate(final IJob<O> job) {
        getCurrentStats().addCreated();
    }

    @Override
    public void onStart(final IJob<O> job) {
    }

    @Override
    public void onSuccess(final IJob<O> job) {
        getCurrentStats().addSuccess();
    }

    @Override
    public void onPostponed(final IJob<O> job) {
        getCurrentStats().addPostponed();
    }

    @Override
    public void onFailed(final IJob<O> job) {
        getCurrentStats().addFailed();
    }

    @Override
    public void clear() {
        stats.clear();
    }

    @Override
    public Map<Long, JobStats> getStats() {
        return stats;
    }

    @Override
    public JobStats getCurrentStats() {
        final long key = buildMapKey();
        return stats.computeIfAbsent(key, JobStats::new);
    }

    private long buildMapKey() {
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        final int truncatedToMinutes = now.getMinute() - now.getMinute() % statsGranularityMinutes;
        return now.withSecond(0).withNano(0).withMinute(truncatedToMinutes).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private static final class JobStatsMap extends LinkedHashMap<Long, JobStats> {

        private final int maxPoints;

        private JobStatsMap(int maxPoints) {
            this.maxPoints = maxPoints;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, JobStats> eldest) {
            return this.size() > maxPoints;
        }
    }
}
