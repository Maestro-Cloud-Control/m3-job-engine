package io.maestro3.job.engine.operation.impl;

import io.maestro3.job.engine.operation.IOperationExecutionHolder;
import io.maestro3.job.engine.operation.OperationKey;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public abstract class AbstractOperationExecutionHolder<O> implements IOperationExecutionHolder<O> {

    public static final int MAX_SIZE = 3;

    private final ExecutionHolder holder = new ExecutionHolder();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    @Override
    public void create(OperationKey key, O operation) {
        writeLock.lock();
        try {
            ExecutionRecord executionRecord = new ExecutionRecord(operation);
            holder.putRecord(key, executionRecord);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void run(OperationKey key, O operation) {
        writeLock.lock();
        try {
            ExecutionRecord executionRecord = holder.get(key, getOperationId(operation));
            if (executionRecord == null || executionRecord.isDone()) {
                executionRecord = new ExecutionRecord(operation);
                holder.putRecord(key, executionRecord);
            } else {
                executionRecord.setOperation(operation);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void write(OperationKey key, O operation, String text) {
        writeLock.lock();
        try {
            ExecutionRecord executionRecord = holder.get(key, getOperationId(operation));
            if (executionRecord == null) {
                throw new IllegalArgumentException("No executions found");
            }
            executionRecord.setText(executionRecord.getText() + text);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void stopRunning(OperationKey key, O operation) {
        writeLock.lock();
        try {
            ExecutionRecord executionRecord = holder.get(key, getOperationId(operation));
            if (executionRecord == null) {
                return;
            }

            executionRecord.setDone(true);
            executionRecord.setText(executionRecord.getText() + "\n Asynchronous execution completed.");
            executionRecord.setOperation(operation);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public O getExecution(String operationId) {
        readLock.lock();
        try {
            return Optional.ofNullable(holder.get(operationId)).map(ExecutionRecord::getOperation).orElse(null);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<O> getExecutionHistory(OperationKey key) {
        readLock.lock();
        try {
            return holder.records(key).stream()
                    .map(ExecutionRecord::getOperation)
                    .sorted(Comparator.comparing(this::getOperationCreationTimestamp).reversed())
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isDone(OperationKey key) {
        readLock.lock();
        try {
            return holder.records(key).stream().allMatch(ExecutionRecord::isDone);
        } finally {
            readLock.unlock();
        }
    }

    protected abstract String getOperationId(O operation);

    protected abstract long getOperationCreationTimestamp(O operation);

    protected abstract O copyOperation(O operation);

    private class ExecutionRecord {

        private String text = "";
        private boolean done;
        private O operation;

        public ExecutionRecord(O operation) {
            this.operation = copyOperation(operation);
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }

        public O getOperation() {
            return copyOperation(operation);
        }

        public void setOperation(O operation) {
            this.operation = copyOperation(operation);
        }

        public String getId() {
            return getOperationId(operation);
        }
    }

    private class ExecutionHolder {

        private final Map<OperationKey, LinkedHashMap<String, ExecutionRecord>> data = new HashMap<>();

        public Collection<ExecutionRecord> records(OperationKey key) {
            return data.computeIfAbsent(key, k -> new ExecutionHolderMap()).values();
        }

        public void putRecord(OperationKey key, ExecutionRecord executionRecord) {
            data.computeIfAbsent(key, k -> new ExecutionHolderMap()).put(executionRecord.getId(), executionRecord);
        }

        public ExecutionRecord get(OperationKey key, String operationId) {
            return data.computeIfAbsent(key, k -> new ExecutionHolderMap()).get(operationId);
        }

        public ExecutionRecord get(String operationId) {
            return data.values().stream()
                    .map(Map::values)
                    .flatMap(Collection::stream)
                    .filter(r -> operationId.equalsIgnoreCase(r.getId()))
                    .findFirst()
                    .orElse(null);
        }
    }

    private class ExecutionHolderMap extends LinkedHashMap<String, ExecutionRecord> {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ExecutionRecord> eldest) {
            return this.size() > MAX_SIZE;
        }
    }
}
