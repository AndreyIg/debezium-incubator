/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.oracle.logminer.valueholder;

import io.debezium.data.Envelope;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/**
 * This class holds LCR data (similar the API of oracle.streams.DefaultRowLCR class). LCR stands for logical change record
 *
 */
public class LogMinerRowLcrImpl implements LogMinerRowLcr {

    private Envelope.Operation commandType;
    private List<LogMinerColumnValue> newLmColumnValues;
    private List<LogMinerColumnValue> oldLmColumnValues;
    private String objectOwner;
    private String objectName;
    private Timestamp sourceTime;
    private String transactionId;
    private BigDecimal scn;

    public LogMinerRowLcrImpl(Envelope.Operation commandType, List<LogMinerColumnValue> newLmColumnValues, List<LogMinerColumnValue> oldLmColumnValues) {
        this.commandType = commandType;
        this.newLmColumnValues = newLmColumnValues;
        this.oldLmColumnValues = oldLmColumnValues;
    }

    @Override
    public Envelope.Operation getCommandType() {
        return commandType;
    }

    @Override
    public List<LogMinerColumnValue> getOldValues() {
        return oldLmColumnValues;
    }

    @Override
    public List<LogMinerColumnValue> getNewValues() {
        return newLmColumnValues;
    }

    @Override
    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public String getObjectOwner() {
        return objectOwner;
    }

    @Override
    public String getObjectName() {
        return objectName;
    }

    @Override
    public void setObjectName(String name) {
        this.objectName = name;
    }

    @Override
    public void setObjectOwner(String name) {
        this.objectOwner = name;
    }

    @Override
    public void setSourceTime(Timestamp changeTime) {
        this.sourceTime = changeTime;
    }

    @Override
    public Timestamp getSourceTime() {
        return sourceTime;
    }

    @Override
    public void setTransactionId(String id) {
        this.transactionId = id;
    }

    @Override
    public BigDecimal getScn() {
        return scn;
    }

    @Override
    public void setScn(BigDecimal scn) {
        this.scn = scn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogMinerRowLcrImpl that = (LogMinerRowLcrImpl) o;
        return commandType == that.commandType &&
                Objects.equals(newLmColumnValues, that.newLmColumnValues) &&
                Objects.equals(oldLmColumnValues, that.oldLmColumnValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandType, newLmColumnValues, oldLmColumnValues);
    }
}

