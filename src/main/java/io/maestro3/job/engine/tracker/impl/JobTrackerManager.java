package io.maestro3.job.engine.tracker.impl;

import io.maestro3.job.engine.model.JobStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.maestro3.job.engine.model.IJob;
import io.maestro3.job.engine.tracker.IJobTracker;
import io.maestro3.job.engine.tracker.IJobTrackerManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class JobTrackerManager<O> implements IJobTrackerManager<O> {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackerManager.class);

    private final Map<String, IJobTracker<O>> jobTrackersMap;

    public JobTrackerManager(List<IJobTracker<O>> jobTrackers) {
        if (jobTrackers == null || jobTrackers.isEmpty()) {
            jobTrackersMap = Collections.emptyMap();
            return;
        }

        jobTrackersMap = new HashMap<>(jobTrackers.size());
        for (final IJobTracker<O> jobTracker : jobTrackers) {
            final String jobType = jobTracker.getSupportedJobType();
            if (jobTrackersMap.containsKey(jobType)) {
                LOG.warn("Found several job trackers for job type: {}", jobTracker.getSupportedJobType());
                continue;
            }
            jobTrackersMap.put(jobType, jobTracker);
        }
    }

    @Override
    public void onCreate(final IJob<O> job) {
        callOnTracker(job.getType(), tracker -> tracker.onCreate(job));
    }

    @Override
    public void onStart(final IJob<O> job) {
        callOnTracker(job.getType(), tracker -> tracker.onStart(job));
    }

    @Override
    public void onSuccess(final IJob<O> job) {
        callOnTracker(job.getType(), tracker -> tracker.onSuccess(job));
    }

    @Override
    public void onPostponed(final IJob<O> job) {
        callOnTracker(job.getType(), tracker -> tracker.onPostponed(job));
    }

    @Override
    public void onFailed(final IJob<O> job) {
        callOnTracker(job.getType(), tracker -> tracker.onFailed(job));
    }

    @Override
    public void clear(final String jobType) {
        callOnTracker(jobType, IJobTracker::clear);
    }

    @Override
    public Map<Long, JobStats> getStats(final String jobType) {
        return getFromTracker(jobType, IJobTracker::getStats);
    }

    @Override
    public JobStats getCurrentStats(final String jobType) {
        return getFromTracker(jobType, IJobTracker::getCurrentStats);
    }

    private void callOnTracker(final String jobType,
                               final Consumer<IJobTracker<O>> consumer) {
        Optional.ofNullable(jobTrackersMap.get(jobType))
                .ifPresent(consumer);
    }

    private <T> T getFromTracker(final String jobType,
                                 final Function<IJobTracker<O>, T> function) {
        return Optional.ofNullable(jobTrackersMap.get(jobType))
                .map(function)
                .orElse(null);
    }
}
