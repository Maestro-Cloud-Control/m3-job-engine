package io.maestro3.job.engine;

import io.maestro3.job.engine.exception.JobExecutionException;
import io.maestro3.job.engine.exception.JobPostponeException;
import io.maestro3.job.engine.model.IJob;
import io.maestro3.job.engine.model.JobStatus;
import io.maestro3.job.engine.model.UpdateJobDataOnError;
import io.maestro3.job.engine.model.UpdateJobDataOnSuccess;
import io.maestro3.job.engine.tracker.IJobTrackerManager;
import io.maestro3.job.engine.util.ExceptionUtils;
import io.maestro3.job.engine.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public abstract class AbstractJobManager<P extends IJobProcessor<?>, O> implements IJobManager {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractJobManager.class);

    protected final IDbJobService<O> jobService;
    protected final Set<String> jobTypes;
    protected final Map<String, IJobProcessor<?>> jobProcessors;
    protected final int maxConcurrentTasks;
    protected final int postponeCount;
    protected final int postponeMinutes;
    protected final int jobsLifeTimeDays;
    protected final boolean usePrioritySort;

    private final IJobExecutionApprover executionApprover;
    private final IJobTrackerManager<O> trackerManager;
    private final ExecutorService executor;
    private final AtomicInteger runningJobs = new AtomicInteger();

    protected AbstractJobManager(IDbJobService<O> jobService,
                                 List<P> jobProcessors,
                                 JobManagerConfiguration<O> configuration) {
        this.jobService = jobService;
        this.jobTypes = jobProcessors.stream()
                .map(jobProcessor -> jobProcessor.getJobDefinition().getProcessorType())
                .collect(Collectors.toSet());
        this.jobProcessors = jobProcessors.stream()
                .collect(Collectors.toMap(jobProcessor -> jobProcessor.getJobDefinition().getProcessorType(), jobProcessor -> jobProcessor));
        this.maxConcurrentTasks = configuration.getMaxConcurrentTasks();
        this.postponeCount = configuration.getPostponeCount();
        this.postponeMinutes = configuration.getPostponeMinutes();
        this.jobsLifeTimeDays = configuration.getJobsLifeTimeDays();
        this.usePrioritySort = configuration.usePrioritySort();
        this.executionApprover = configuration.getExecutionApprover();
        this.trackerManager = configuration.getTrackerManager();

        executor = Optional.ofNullable(configuration.getExecutor())
                .orElseGet(() -> new ThreadPoolExecutor(1, maxConcurrentTasks, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(), configuration.getThreadFactory()));
    }

    @Override
    public void clearExecutedJobs() {
        try {
            int removedJobs = jobService.deleteExecutedJobs(jobsLifeTimeDays);
            LOG.info("[JobManager]: Cron to clear success jobs was executed. Removed {} jobs", removedJobs);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void executeNextPendingJob() {
        try {
            if (!canExecuteNextPendingJob()) {
                return;
            }

            final IJob<O> job = jobService.getNextJobForRun(jobTypes, false, usePrioritySort);
            if (job == null) {
                LOG.trace("Jobs for execution are not found, exiting.");
                return;
            }

            if (skipJob(job)) {
                LOG.trace("Job {} need to be skipped.", job.getId());
                onSkipJob(job);
                return;
            }

            final IJobProcessor<?> jobProcessor = jobProcessors.get(job.getType());
            submitJob(job, jobProcessor);
        } catch (Exception e) {
            LOG.error("Failure during job execution, cause: {}", e.getMessage());
        }
    }

    private boolean canExecuteNextPendingJob() {
        final boolean executionApproved = Optional.ofNullable(executionApprover)
                .map(IJobExecutionApprover::approveJobExecution)
                .orElse(true);
        if (!executionApproved) {
            LOG.debug("Job execution was not approved");
            return false;
        }
        if (runningJobs.get() >= maxConcurrentTasks) {
            LOG.info("Running job count {} exceed max concurrent tasks threshold {}", runningJobs.get(), maxConcurrentTasks);
            return false;
        }
        return true;
    }

    protected boolean skipJob(final IJob<O> job) {
        return false;
    }

    protected void onSkipJob(final IJob<O> job) {
        // postpone skipped job by default
        tryPostponeJob(job, null);
        updateDbJob(job);
    }

    protected void tryPostponeJob(final IJob<O> job, final Integer customPostponeMinutes) {
        if (job.getPostponeCount() >= this.postponeCount) {
            onPostponeCountExceeded(job);
        } else {
            onPostponeAvailable(job, customPostponeMinutes);
        }
    }

    protected void onPostponeCountExceeded(final IJob<O> job) {
        job.setStatus(JobStatus.FAILED);
        trackStat(job, IJobTrackerManager::onFailed);
    }

    protected void onPostponeAvailable(final IJob<O> job, final Integer customPostponeMinutes) {
        Date date = getPostponedDate(customPostponeMinutes);
        LOG.info("Postponing job with id: {}, of type: {}, next executing time: {}", job.getId(), job.getType(), date);
        job.setDate(date);
        job.setStatus(JobStatus.POSTPONED);
        job.incPostponeCount();
        trackStat(job, IJobTrackerManager::onPostponed);
    }

    private Date getPostponedDate(final Integer customPostponeMinutes) {
        final LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC)
                .plusMinutes(Optional.ofNullable(customPostponeMinutes).orElse(postponeMinutes));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

    private void submitJob(final IJob<O> job, final IJobProcessor<?> processor) {
        try {
            final Runnable runner = new JobRunner<>(job, processor);
            executor.execute(runner);
        } catch (RejectedExecutionException e) {
            LOG.warn("Executor rejected task. Job id: {} . Active running jobs count {}. Message {}",
                    job.getId(), runningJobs.get(), e.getMessage());
        } catch (Exception e) {
            LOG.error("Internal Job exception error", e);
        }
    }

    protected void onStart(final IJob<O> job) {
        trackStat(job, IJobTrackerManager::onStart);
    }

    protected void markJobAsSuccess(final IJob<O> job) {
        job.setStatus(JobStatus.SUCCESS);
        updateDbJob(job);
        trackStat(job, IJobTrackerManager::onSuccess);
    }

    protected void markJobAsFailed(final IJob<O> job, Exception e) {
        addErrorToJob(job, e);
        job.setStatus(JobStatus.FAILED);
        updateDbJob(job);
        trackStat(job, IJobTrackerManager::onFailed);
    }

    protected void onFinish(final IJob<O> job) {
    }

    protected void addErrorToJob(final IJob<O> job, final Exception e) {
        job.setLastErrorMessage(ExceptionUtils.exceptionToString(e));
        job.setLastErrorDate(new Date());
    }

    private void updateDbJob(final IJob<O> job) {
        job.setProcessedDate(new Date());
        jobService.saveJob(job);
    }

    private void trackStat(final IJob<O> job,
                           final BiConsumer<IJobTrackerManager<O>, IJob<O>> biConsumer) {
        Optional.ofNullable(trackerManager).ifPresent(manager -> biConsumer.accept(manager, job));
    }

    /**
     * Internal JobRunner  class
     */
    private final class JobRunner<T> implements Runnable {

        private final IJob<O> job;
        private final IJobProcessor<T> jobProcessor;
        private T jobData;

        JobRunner(IJob<O> job, IJobProcessor<T> jobProcessor) {
            this.job = job;
            this.jobProcessor = jobProcessor;
        }

        @Override
        public void run() {
            try {
                onStart(job);
                runningJobs.incrementAndGet();
                LOG.info("Executing job with id: {}", job.getId());
                jobData = JsonUtils.parseJson(job.getData(), jobProcessor.getJobDefinition().getClassReference());
                final Object result = jobProcessor.call(jobData);
                onSuccess(result);
            } catch (Exception e) {
                onError(e);
            } finally {
                onFinish(job);
                runningJobs.decrementAndGet();
            }
        }

        void onSuccess(final Object result) {
            if (jobProcessor instanceof UpdateJobDataOnSuccess) {
                updateJobData(false);
            }
            job.setResult(Optional.ofNullable(result).map(JsonUtils::convertToJson).orElse(null));
            markJobAsSuccess(job);
        }

        void onError(final Exception e) {
            if (jobProcessor instanceof UpdateJobDataOnError) {
                updateJobData(true);
            }
            job.setResult(e.getMessage());
            if (e instanceof JobPostponeException) {
                LOG.warn("Postponed job execution with id: {}. Cause: {}", job.getId(), e.getMessage());
                tryPostponeJob(job, e);
            } else if (e instanceof JobExecutionException) {
                LOG.error("Failed job execution with id: {}. Cause: {}", job.getId(), e.getMessage(), e);
                markJobAsFailed(job, e);
            } else {
                LOG.error("Unexpected failed job execution with id: {}. Cause: {}", job.getId(), e.getMessage(), e);
                markJobAsFailed(job, e);
            }
        }

        void updateJobData(boolean quietly) {
            try {
                job.setData(JsonUtils.convertToJson(jobData));
            } catch (Exception e) {
                if (quietly) {
                    LOG.error("Failed to update job data with id: {}", job.getId(), e);
                } else {
                    throw e;
                }
            }
        }

        private void tryPostponeJob(final IJob<O> job, final Exception t) {
            addErrorToJob(job, t);
            Integer customPostponeMinutes = null;
            if (t instanceof JobPostponeException) {
                customPostponeMinutes = ((JobPostponeException) t).getCustomPostponeMinutes();
            }
            AbstractJobManager.this.tryPostponeJob(job, customPostponeMinutes);
            updateDbJob(job);
        }
    }
}