/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.oracle.jsqlparser;

import io.debezium.connector.oracle.antlr.listener.ParserUtils;
import io.debezium.connector.oracle.logminer.OracleChangeRecordValueConverter;
import io.debezium.connector.oracle.logminer.valueholder.ColumnValueHolder;
import io.debezium.connector.oracle.logminer.valueholder.LogMinerColumnValue;
import io.debezium.connector.oracle.logminer.valueholder.LogMinerColumnValueImpl;
import io.debezium.connector.oracle.logminer.valueholder.LogMinerRowLcr;
import io.debezium.connector.oracle.logminer.valueholder.LogMinerRowLcrImpl;
import io.debezium.data.Envelope;
import io.debezium.relational.Column;
import io.debezium.relational.Table;
import io.debezium.relational.Tables;
import io.debezium.text.ParsingException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * This class does parsing of simple DML: insert, update, delete.
 * Log Miner supplies very simple syntax , that this parser should be sufficient to parse those.
 * It does no support joins, merge, sub-selects and other complicated cases, which should be OK for Log Miner case
 */
public class SimpleDmlParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDmlParser.class);
    protected String catalogName;
    protected String schemaName;
    protected Table table;
    private final OracleChangeRecordValueConverter converter;
    private String aliasName;
    private LogMinerRowLcr rowLCR;
    private Map<String, ColumnValueHolder> newColumnValues = new LinkedHashMap<>();
    private Map<String, ColumnValueHolder> oldColumnValues = new LinkedHashMap<>();
    private CCJSqlParserManager pm;

    /**
     * get parsed DML as an object
     * @return this object
     */
    public LogMinerRowLcr getDmlChange() {
        return rowLCR;
    }

    /**
     * Constructor
     * @param catalogName database name
     * @param schemaName user name
     * @param converter value converter
     */
    public SimpleDmlParser(String catalogName, String schemaName, OracleChangeRecordValueConverter converter) {
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.converter = converter;
        pm = new CCJSqlParserManager();
    }

    /**
     * This parses a DML
     * @param dmlContent DML
     * @param tables debezium Tables
     */
    public void parse(String dmlContent, Tables tables, String txId){
        try {
            if (dmlContent == null) {
                LOGGER.error("Cannot parse NULL , transaction: {}", txId);
                rowLCR = null;
                return;
            }
            // todo investigate: happens on CTAS
            if (dmlContent.endsWith(";null;")) {
                dmlContent = dmlContent.substring(0, dmlContent.lastIndexOf(";null;"));
            }
            if (!dmlContent.endsWith(";")) {
                dmlContent = dmlContent + ";";
            }

            newColumnValues.clear();
            oldColumnValues.clear();

            Statement st = pm.parse(new StringReader(dmlContent));
            if (st instanceof Update){
                parseUpdate(tables, (Update) st);
                List<LogMinerColumnValue> actualNewValues = newColumnValues.values().stream()
                        .filter(ColumnValueHolder::isProcessed).map(ColumnValueHolder::getColumnValue).collect(Collectors.toList());
                List<LogMinerColumnValue> actualOldValues = oldColumnValues.values().stream()
                        .filter(ColumnValueHolder::isProcessed).map(ColumnValueHolder::getColumnValue).collect(Collectors.toList());
                rowLCR = new LogMinerRowLcrImpl(Envelope.Operation.UPDATE, actualNewValues, actualOldValues);

            } else if (st instanceof Insert) {
                parseInsert(tables, (Insert) st);
                List<LogMinerColumnValue> actualNewValues = newColumnValues.values()
                        .stream().map(ColumnValueHolder::getColumnValue).collect(Collectors.toList());
                rowLCR = new LogMinerRowLcrImpl(Envelope.Operation.CREATE, actualNewValues, Collections.emptyList());

            } else if (st instanceof Delete) {
                parseDelete(tables, (Delete) st);
                List<LogMinerColumnValue> actualOldValues = oldColumnValues.values()
                        .stream().map(ColumnValueHolder::getColumnValue).collect(Collectors.toList());
                rowLCR = new LogMinerRowLcrImpl(Envelope.Operation.DELETE, Collections.emptyList(), actualOldValues);

            } else {
                throw new UnsupportedOperationException("Operation " + st + " is not supported yet");
            }

        } catch (Throwable e) {
            LOGGER.error("Cannot parse statement : {}, transaction: {}, due to the {}", dmlContent, txId, e);
            rowLCR = null;
        }

    }

    private void initColumns(Tables tables, String tableName) {
        table = tables.forTable(catalogName, schemaName, tableName);
        if (table == null) {
            throw new ParsingException(null, "Trying to parse a table, which does not exist.");
        }
        for (int i = 0; i < table.columns().size(); i++) {
            Column column = table.columns().get(i);
            int type = column.jdbcType();
            String key = column.name();
            String name = ParserUtils.stripeQuotes(column.name().toUpperCase());
            newColumnValues.put(key, new ColumnValueHolder(new LogMinerColumnValueImpl(name, type)));
            oldColumnValues.put(key, new ColumnValueHolder(new LogMinerColumnValueImpl(name, type)));
        }
    }

    // this parses simple statement with only one table
    private void parseUpdate(Tables tables, Update st) throws JSQLParserException {
        int tableCount = st.getTables().size();
        if (tableCount > 1 || tableCount == 0){
            throw new JSQLParserException("DML includes " + tableCount + " tables");
        }
        net.sf.jsqlparser.schema.Table parseTable  = st.getTables().get(0);
        initColumns(tables, ParserUtils.stripeQuotes(parseTable.getName()));

        List<net.sf.jsqlparser.schema.Column> columns = st.getColumns();
        Alias alias = parseTable.getAlias();
        aliasName = alias == null ? "" : alias.getName().trim();

        List<Expression> expressions = st.getExpressions(); // new values
        if (expressions.size() != columns.size()){
            throw new JSQLParserException("DML has " + expressions.size() + " column values, but Table object has " + columns.size() + " columns");
        }
        setNewValues(expressions, columns);
        Expression where = st.getWhere(); //old values
        if (where != null) {
            parseWhereClause(where);
            ParserUtils.cloneOldToNewColumnValues(newColumnValues, oldColumnValues, table);
        } else {
            oldColumnValues.clear();
        }
    }

    private void parseInsert(Tables tables, Insert st) {
        initColumns(tables, ParserUtils.stripeQuotes(st.getTable().getName()));
        Alias alias = st.getTable().getAlias();
        aliasName = alias == null ? "" : alias.getName().trim();

        List<net.sf.jsqlparser.schema.Column> columns = st.getColumns();
        ItemsList values = st.getItemsList();
        values.accept(new ItemsListVisitorAdapter() {
            @Override
            public void visit(ExpressionList expressionList) {
                super.visit(expressionList);
                List<Expression> expressions = expressionList.getExpressions();
                setNewValues(expressions, columns);
            }
        });
        oldColumnValues.clear();
    }

    private void parseDelete(Tables tables, Delete st) {
        initColumns(tables, ParserUtils.stripeQuotes(st.getTable().getName()));
        newColumnValues.clear();

        Expression where = st.getWhere();
        if (where != null) {
            parseWhereClause(where);
        } else {
            oldColumnValues.clear();
        }
    }

    private void setNewValues(List<Expression> expressions, List<net.sf.jsqlparser.schema.Column> columns) {
        if (expressions.size() != columns.size()){
            throw new RuntimeException("DML has " + expressions.size() + " column values, but Table object has " + columns.size() + " columns");
        }

        for (int i = 0; i < columns.size(); i++) {
            String columnName = ParserUtils.stripeQuotes(columns.get(i).getColumnName().toUpperCase());
            String value = ParserUtils.stripeQuotes(expressions.get(i).toString());
            Object stripedValue = ParserUtils.removeApostrophes(value);
            Column column = table.columnWithName(columnName);
            Object valueObject = ParserUtils.convertValueToSchemaType(column, stripedValue, converter);

            ColumnValueHolder columnValueHolder = newColumnValues.get(columnName);
            columnValueHolder.setProcessed(true);
            columnValueHolder.getColumnValue().setColumnData(valueObject);
        }
    }

    private void parseWhereClause(Expression logicalExpression)  {

        logicalExpression.accept(new ExpressionVisitorAdapter(){
            @Override
            public void visit(EqualsTo expr) {
                super.visit(expr);
                String columnName  = expr.getLeftExpression().toString();
                columnName = ParserUtils.stripeAlias(columnName, aliasName);
                String value = expr.getRightExpression().toString();
                columnName = ParserUtils.stripeQuotes(columnName);

                Column column = table.columnWithName(columnName);
                value = ParserUtils.removeApostrophes(value);

                ColumnValueHolder columnValueHolder = oldColumnValues.get(columnName.toUpperCase());
                if (columnValueHolder != null) {
                    Object valueObject = ParserUtils.convertValueToSchemaType(column, value, converter);
                    columnValueHolder.setProcessed(true);
                    columnValueHolder.getColumnValue().setColumnData(valueObject);
                }
            }


            @Override
            public void visit(IsNullExpression expr) {
                super.visit(expr);
                String columnName  = expr.getLeftExpression().toString();
                columnName = ParserUtils.stripeAlias(columnName, aliasName);
                columnName = ParserUtils.stripeQuotes(columnName);
                ColumnValueHolder columnValueHolder = oldColumnValues.get(columnName.toUpperCase());
                if (columnValueHolder != null) {
                    columnValueHolder.setProcessed(true);
                    columnValueHolder.getColumnValue().setColumnData(null);
                }
            }
        });
    }
}