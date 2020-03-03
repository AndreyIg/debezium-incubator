/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.oracle.logminer;

/**
 * This interface is exposed for JMX
 */
public interface LogMinerMetricsMXBean {

    /**
     * Exposes current SCN in the database. This is very efficient query and will not affect overall performance
     *
     * @return current SCN
     */
    Long getCurrentScn();

    /**
     * Exposes current redo log file. This is very efficient query and will not affect overall performance
     *
     * @return full path or NULL if an exception occurs.
     */
    String getCurrentRedoLogFileName();

    /**
     * Exposes states of redo logs: current, active, inactive, unused ...
     * @return array of: (redo log name | status) elements
     */
    String[] getRedoLogStatus();

    /**
     * fetches counter of redo switches for the last day.
     * If this number is high , like once in 3 minutes,  the troubleshooting on the database level is required.
     * @return counter
     */
    int getSwitchCounter();

    /**
     * @return number of milliseconds last Log Miner query took
     */
    Long getLastLogMinerQueryDuration();

    /**
     * @return number of captured DML since the connector is up
     */
    int getCapturedDmlCount();

    /**
     * @return number of Log Miner view queries since the connector is up
     */
    int getLogMinerQueryCount();

    /**
     * @return average duration of Log Miner view query
     */
    Long getAverageLogMinerQueryDuration();

    /**
     * Log Miner view query returns number of captured DML , Commit and Rollback. This is what we call a batch.
     * @return duration of the last batch processing, which includes parsing and dispatching
     */
    Long getLastProcessedCapturedBatchDuration();

    /**
     * Log Miner view query returns number of captured DML , Commit and Rollback. This is what we call a batch.
     * @return number of all processed batches , which includes parsing and dispatching
     */
    int getProcessedCapturedBatchCount();

    /**
     * @return average time of processing captured batch from Log Miner view
     */
    Long getAverageProcessedCapturedBatchDuration();

    /**
     * Maximum number of entries in Log Miner view to fetch. This is used to set the diapason of the SCN in mining query.
     * If difference between "start SCN" and "end SCN" to mine exceeds this limit, end SCN will be set to "start SCN" + maxBatchSize
     * @return the limit
     */
    int getMaxBatchSize();

    /**
     * this gives ability to manipulate maximum number of entries in Log Miner view to fetch.
     * It has limits to prevent abnormal values
     * @param size limit
     */
    void setMaxBatchSize(int size);

    /**
     * @return number of milliseconds for connector to sleep before fetching another batch from the Log Miner view
     */
    int getMillisecondToSleepBetweenMiningQuery();

    /**
     * sets number of milliseconds for connector to sleep before fetching another batch from the Log Miner view
     * @param milliseconds to sleep
     */
    void setMillisecondToSleepBetweenMiningQuery(int milliseconds);

    /**
     * @return number of fetched records from Log Miner view. It serves as a trigger point for connector to sleep.
     * This helps in reducing database impact by mining query by making it less frequent
     */
    int getFetchedRecordSizeLimitToFallAsleep();

    /**
     * sets the limit of fetched records from Log Miner view.
     * @param size number of records
     */
    void setFetchedRecordSizeLimitToFallAsleep(int size);
}
