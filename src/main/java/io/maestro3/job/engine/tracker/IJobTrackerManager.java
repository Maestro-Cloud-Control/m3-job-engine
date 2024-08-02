package io.maestro3.job.engine.tracker;

import io.maestro3.job.engine.model.JobStats;
import io.maestro3.job.engine.model.IJob;

import java.util.Map;

public interface IJobTrackerManager<O> {

    void onCreate(IJob<O> job);

    void onStart(IJob<O> job);

    void onSuccess(IJob<O> job);

    void onPostponed(IJob<O> job);

    void onFailed(IJob<O> job);

    void clear(String jobType);

    Map<Long, JobStats> getStats(String jobType);

    JobStats getCurrentStats(String jobType);

}
