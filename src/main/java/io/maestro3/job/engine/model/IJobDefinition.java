package io.maestro3.job.engine.model;

import com.fasterxml.jackson.core.type.TypeReference;

public interface IJobDefinition<D> {

    String getProcessorType();

    /**
     * Used for deserializing data for job execution
     */
    TypeReference<D> getClassReference();

    /**
     * This flag will trigger generation of 'operationId' field in form of UUID
     * @return true if the job definition type should be supported by OperationHandler
     */
    default boolean isOperational() { return false; }
}