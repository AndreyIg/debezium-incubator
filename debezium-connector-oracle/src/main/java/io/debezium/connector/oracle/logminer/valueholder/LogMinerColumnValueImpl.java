/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.oracle.logminer.valueholder;

import io.debezium.connector.oracle.antlr.listener.ParserUtils;

import java.util.Objects;

/**
 * This class mimics the API of oracle.streams.DefaultColumnValue implementation
 *
 */
public class LogMinerColumnValueImpl implements LogMinerColumnValue {

    private String columnName;
    private Object columnData;
    private int columnType;

    public LogMinerColumnValueImpl(String columnName, int columnType) {
        this.columnName = columnName;
        this.columnType = columnType;
    }

    @Override
    public int getColumnDataType() {
        return columnType;
    }

    @Override
    public Object getColumnData() {
        return columnData;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public void setColumnData(Object columnData) {
        if (columnData instanceof String) {
            this.columnData = ParserUtils.replaceDoubleBackSlashes((String) columnData);
        } else {
            this.columnData = columnData;
        }
    }

    @Override
    public String toString() {
        return "LogMinerColumnValueImpl{" +
                "columnName='" + columnName + '\'' +
                ", columnData=" + columnData +
                ", columnType=" + columnType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogMinerColumnValueImpl that = (LogMinerColumnValueImpl) o;
        return columnType == that.columnType &&
                Objects.equals(columnName, that.columnName) &&
                Objects.equals(columnData, that.columnData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, columnData, columnType);
    }
}
