package io.maestro3.job.engine.model;

import java.util.Date;

public interface IJob<O> {

    int MAX_PRIORITY_VALUE = 5;
    int MIN_PRIORITY_VALUE = 1;
    int DEFAULT_PRIORITY_VALUE = 3;

    String getId();

    String getType();

    void setType(String type);

    String getData();

    void setData(String data);

    JobStatus getStatus();

    void setStatus(JobStatus status);

    Date getDate();

    void setDate(Date date);

    String getLastErrorMessage();

    void setLastErrorMessage(String lastErrorMessage);

    Date getCreatedDate();

    void setCreatedDate(Date createdDate);

    Date getStartedDate();

    void setStartedDate(Date startedDate);

    Date getProcessedDate();

    void setProcessedDate(Date processedDate);

    Date getLastErrorDate();

    void setLastErrorDate(Date lastErrorDate);

    int getPostponeCount();

    void setPostponeCount(int postponeCount);

    void incPostponeCount();

    int getPriority();

    void setPriority(int priority);

    String getOperationId();

    void setOperationId(String operationId);

    String getResult();

    void setResult(String result);

    O getOperation();
}
