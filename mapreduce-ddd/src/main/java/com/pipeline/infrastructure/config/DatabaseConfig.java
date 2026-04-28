package com.pipeline.infrastructure.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.hadoop.conf.Configuration;

public class DatabaseConfig {

    private final String url;
    private final String user;
    private final String password;
    private Connection cachedConnection;

    public DatabaseConfig(Configuration hadoopConf) {
        this.url = hadoopConf.get("pipeline.db.url");
        this.user = hadoopConf.get("pipeline.db.user");
        this.password = hadoopConf.get("pipeline.db.password");

        if (url == null || user == null) {
            throw new IllegalStateException(
                    "Database configuration missing. Ensure POSTGRES_URL/USER/PASSWORD are set in .env");
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (cachedConnection == null || cachedConnection.isClosed()) {
            cachedConnection = DriverManager.getConnection(url, user, password);
            cachedConnection.setAutoCommit(false);
        }
        return cachedConnection;
    }

    public synchronized void commit() throws SQLException {
        if (cachedConnection != null && !cachedConnection.isClosed()) {
            cachedConnection.commit();
        }
    }

    public synchronized void close() {
        if (cachedConnection != null) {
            try {
                cachedConnection.commit();
                cachedConnection.close();
            } catch (SQLException ignored) {}
            cachedConnection = null;
        }
    }
}
