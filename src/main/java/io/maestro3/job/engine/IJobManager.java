package io.maestro3.job.engine;

public interface IJobManager {

    /**
     * Clears jobs that were successfully executed
     */
    void clearExecutedJobs();

    /**
     * Executes next pending job in execution queue
     */
    void executeNextPendingJob();

}