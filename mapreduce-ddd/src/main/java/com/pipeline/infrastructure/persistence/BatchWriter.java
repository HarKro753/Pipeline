package com.pipeline.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BatchWriter implements AutoCloseable {

    private static final int BATCH_SIZE = 1000;

    private final Connection connection;
    private final PreparedStatement statement;
    private int pendingCount = 0;

    public BatchWriter(Connection connection, String sql) throws SQLException {
        this.connection = connection;
        this.statement = connection.prepareStatement(sql);
    }

    public PreparedStatement getStatement() {
        return statement;
    }

    public void addToBatch() throws SQLException {
        statement.addBatch();
        pendingCount++;
        if (pendingCount >= BATCH_SIZE) {
            flush();
        }
    }

    public void flush() throws SQLException {
        if (pendingCount > 0) {
            statement.executeBatch();
            connection.commit();
            pendingCount = 0;
        }
    }

    @Override
    public void close() throws SQLException {
        flush();
        statement.close();
    }
}
