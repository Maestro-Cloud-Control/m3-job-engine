package io.maestro3.job.engine;

@FunctionalInterface
public interface IJobExecutionApprover {

    boolean approveJobExecution();
}
