package io.maestro3.job.engine;

import io.maestro3.job.engine.model.IJob;

import java.util.Set;

public interface IDbJobService<O> {

    /**
     * Removes all success jobs for interval
     *
     * @param daysInterval interval for which we delete jobs
     */
    int deleteExecutedJobs(int daysInterval);

    IJob<O> getNextJobForRun(Set<String> processorTypes, boolean excludeTypes, boolean sortByPriority);

    void saveJob(IJob<O> job);
}
