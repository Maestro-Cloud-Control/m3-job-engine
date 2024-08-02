package io.maestro3.job.engine.model;

import java.util.concurrent.atomic.AtomicInteger;

public class JobStats {
    private final long time;
    private final AtomicInteger created;
    private final AtomicInteger success;
    private final AtomicInteger failed;
    private final AtomicInteger postponed;

    public JobStats(long time) {
        this.time = time;
        this.created = new AtomicInteger();
        this.success = new AtomicInteger();
        this.failed = new AtomicInteger();
        this.postponed = new AtomicInteger();
    }

    public int getCreated() {
        return created.get();
    }

    public int getSuccess() {
        return success.get();
    }

    public int getFailed() {
        return failed.get();
    }

    public int getPostponed() {
        return postponed.get();
    }

    public long getTime() {
        return time;
    }

    public void addCreated() {
        created.incrementAndGet();
    }

    public void addSuccess() {
        success.incrementAndGet();
    }

    public void addFailed() {
        failed.incrementAndGet();
    }

    public void addPostponed() {
        postponed.incrementAndGet();
    }
}
