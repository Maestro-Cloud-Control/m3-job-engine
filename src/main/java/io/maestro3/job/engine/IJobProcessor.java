package io.maestro3.job.engine;

import io.maestro3.job.engine.exception.JobExecutionException;
import io.maestro3.job.engine.model.IJobDefinition;

import java.util.List;

public interface IJobProcessor<D> {
    /**
     * Returns definition of job that processor can execute
     *
     * @return
     */
    IJobDefinition<D> getJobDefinition();

    /**
     * Executes job
     *
     * @param data specified data for processing
     * @throws JobExecutionException when fails to execute job
     */
    default void execute(D data) throws Exception {
    }

    default void clean(List<D> data) throws Exception {
    }

    default Object call(D data) throws Exception {
        execute(data);
        return null;
    }
}
