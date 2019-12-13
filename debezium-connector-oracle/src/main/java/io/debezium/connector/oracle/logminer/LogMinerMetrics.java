/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.oracle.logminer;

import io.debezium.annotation.ThreadSafe;
import io.debezium.connector.common.CdcSourceTaskContext;
import io.debezium.metrics.Metrics;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class contains methods to be exposed via MBean server
 *
 */
@ThreadSafe
public class LogMinerMetrics extends Metrics implements LogMinerMetricsMXBean {
    private AtomicLong currentScn = new AtomicLong();
    private AtomicInteger capturedDmlCount = new AtomicInteger();
    private AtomicReference<String> currentLogFileName;
    private AtomicReference<String[]> redoLogStatus;
    private AtomicInteger switchCounter = new AtomicInteger();
    private AtomicReference<Duration> lastLogMinerQueryDuration = new AtomicReference<>();
    private AtomicReference<Duration> averageLogMinerQueryDuration = new AtomicReference<>();
    private AtomicInteger logMinerQueryCount = new AtomicInteger();
    private AtomicReference<Duration> lastProcessedCapturedBatchDuration = new AtomicReference<>();
    private AtomicInteger processedCapturedBatchCount = new AtomicInteger();
    private AtomicReference<Duration> averageProcessedCapturedBatchDuration = new AtomicReference<>();
    private AtomicInteger maxBatchSize = new AtomicInteger();
    private AtomicInteger millisecondToSleepBetweenMiningQuery = new AtomicInteger();
    private AtomicInteger fetchedRecordSizeLimitToFallAsleep = new AtomicInteger();
    private AtomicBoolean ctas = new AtomicBoolean();

    LogMinerMetrics(CdcSourceTaskContext taskContext) {
        super(taskContext, "log-miner");

        maxBatchSize.set(2000);
        millisecondToSleepBetweenMiningQuery.set(500);
        fetchedRecordSizeLimitToFallAsleep.set(50);

        currentScn.set(-1);
        capturedDmlCount.set(0);
        currentLogFileName = new AtomicReference<>();
        redoLogStatus = new AtomicReference<>();
        switchCounter.set(0);
        averageLogMinerQueryDuration.set(Duration.ZERO);
        lastLogMinerQueryDuration.set(Duration.ZERO);
        logMinerQueryCount.set(0);
        lastProcessedCapturedBatchDuration.set(Duration.ZERO);
        processedCapturedBatchCount.set(0);
        averageProcessedCapturedBatchDuration.set(Duration.ZERO);

        ctas.set(false);
    }

    // setters
    public void setCurrentScn(Long scn){
        currentScn.set(scn);
    }

    public void setCapturedDmlCount(int count){
        capturedDmlCount.set(count);
    }

    public void setCurrentLogFileName(String name){
        currentLogFileName.set(name);
    }

    public void setRedoLogStatus(Map<String, String> status){
        String[] statusArray = status.entrySet().stream().map(e -> e.getKey() + " | " + e.getValue()).toArray(String[]::new);
        redoLogStatus.set(statusArray);
    }

    public void setSwitchCount(int counter) {
        switchCounter.set(counter);
    }

    public void setLastLogMinerQueryDuration(Duration fetchDuration){
        setDurationMetrics(fetchDuration, lastLogMinerQueryDuration, logMinerQueryCount, averageLogMinerQueryDuration);
    }

    public void setProcessedCapturedBatchDuration(Duration processDuration){
        setDurationMetrics(processDuration, lastProcessedCapturedBatchDuration, processedCapturedBatchCount, averageProcessedCapturedBatchDuration);
    }

    // implemented getters
    @Override
    public Long getCurrentScn() {
        return currentScn.get();
    }

    @Override
    public int getCapturedDmlCount() {
        return capturedDmlCount.get();
    }

    @Override
    public String getCurrentRedoLogFileName() {
        return currentLogFileName.get();
    }

    @Override
    public String[] getRedoLogStatus() {
        return redoLogStatus.get();
    }

    @Override
    public int getSwitchCounter() {
        return switchCounter.get();
    }

    @Override
    public Long getLastLogMinerQueryDuration() {
        return lastLogMinerQueryDuration.get() == null ? 0 : lastLogMinerQueryDuration.get().toMillis();
    }

    @Override
    public Long getAverageLogMinerQueryDuration() {
        return averageLogMinerQueryDuration.get() == null ? 0 : averageLogMinerQueryDuration.get().toMillis();
    }

    @Override
    public Long getLastProcessedCapturedBatchDuration() {
        return lastProcessedCapturedBatchDuration.get() == null ? 0 : lastProcessedCapturedBatchDuration.get().toMillis();
    }

    @Override
    public int getLogMinerQueryCount() {
        return logMinerQueryCount.get();
    }

    @Override
    public int getProcessedCapturedBatchCount() {
        return processedCapturedBatchCount.get();
    }

    @Override
    public Long getAverageProcessedCapturedBatchDuration() {
        return averageProcessedCapturedBatchDuration.get() == null ? 0 : averageProcessedCapturedBatchDuration.get().toMillis();
    }

    @Override
    public int getMaxBatchSize() {
        return maxBatchSize.get();
    }

    @Override
    public int getMillisecondToSleepBetweenMiningQuery() {
        return millisecondToSleepBetweenMiningQuery.get();
    }

    @Override
    public int getFetchedRecordSizeLimitToFallAsleep() {
        return fetchedRecordSizeLimitToFallAsleep.get();
    }

    // MBean accessible setters
    @Override
    public void setMaxBatchSize(int size) {
        if (size >= 100 && size <= 10_000) {
            maxBatchSize.set(size);
        }
    }

    @Override
    public void setMillisecondToSleepBetweenMiningQuery(int milliseconds) {
        if (milliseconds >= 10 && milliseconds <= 2000){
            millisecondToSleepBetweenMiningQuery.set(milliseconds);
        }
    }

    @Override
    public void setFetchedRecordSizeLimitToFallAsleep(int size) {
        if (size >= 50 && size <= 200) {
            fetchedRecordSizeLimitToFallAsleep.set(size);
        }
    }

    @Override
    public boolean getCTAS() {
        return ctas.get();
    }

    @Override
    public void setCTAS(boolean ctas) {
//        this.ctas.set(ctas);
    }

    // private methods
    private void setDurationMetrics(Duration duration, AtomicReference<Duration> lastDuration, AtomicInteger counter,
                                    AtomicReference<Duration> averageDuration){
        if (duration != null) {
            lastDuration.set(duration);
            int count = counter.incrementAndGet();
            Duration currentAverage = averageDuration.get() == null ? Duration.ZERO : averageDuration.get();
            averageDuration.set(currentAverage.multipliedBy(count - 1).plus(duration).dividedBy(count));
        }
    }

    @Override
    public String toString() {
        return "LogMinerMetrics{" +
                "currentScn=" + currentScn.get() +
                ", currentLogFileName=" + currentLogFileName.get() +
                ", redoLogStatus=" + Arrays.toString(redoLogStatus.get()) +
                ", capturedDmlCount=" + capturedDmlCount.get() +
                ", switchCounter=" + switchCounter.get() +
                ", lastLogMinerQueryDuration=" + lastLogMinerQueryDuration.get() +
                ", logMinerQueryCount=" + logMinerQueryCount.get() +
                ", averageLogMinerQueryDuration=" + averageLogMinerQueryDuration.get() +
                ", lastProcessedCapturedBatchDuration=" + lastProcessedCapturedBatchDuration.get() +
                ", processedCapturedBatchCount=" + processedCapturedBatchCount.get() +
                ", averageProcessedCapturedBatchDuration=" + averageProcessedCapturedBatchDuration.get() +
                ", maxBatchSize=" + maxBatchSize.get() +
                ", millisecondToSleepBetweenMiningQuery=" + millisecondToSleepBetweenMiningQuery.get() +
                ", maxBatchSize=" + maxBatchSize.get() +
                '}';
    }
}