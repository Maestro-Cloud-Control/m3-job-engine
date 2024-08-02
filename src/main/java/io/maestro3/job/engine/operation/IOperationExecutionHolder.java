package io.maestro3.job.engine.operation;

import java.util.Collection;

public interface IOperationExecutionHolder<O> {

    void create(OperationKey key, O operation);

    void run(OperationKey key, O operation);

    void write(OperationKey operationKey, O operation, String text);

    void stopRunning(OperationKey key, O operation);

    O getExecution(String operationId);

    Collection<O> getExecutionHistory(OperationKey key);

    boolean isDone(OperationKey key);
}
