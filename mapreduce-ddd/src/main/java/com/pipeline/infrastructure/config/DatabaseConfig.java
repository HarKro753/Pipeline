package com.pipeline.infrastructure.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Loads Postgres connection settings from .env and provides connection pooling.
 * Reuses a single connection per thread to avoid connection churn at TB scale.
 */
public class DatabaseConfig {

    private final String url;
    private final String user;
    private final String password;
    private Connection cachedConnection;

    public DatabaseConfig() {
        Dotenv dotenv = Dotenv.configure()
                .directory("../")
                .filename(".env")
                .load();

        this.url = dotenv.get("POSTGRES_URL");
        this.user = dotenv.get("POSTGRES_USER");
        this.password = dotenv.get("POSTGRES_PASSWORD");
    }

    public synchronized Connection getConnection() throws SQLException {
        if (cachedConnection == null || cachedConnection.isClosed()) {
            cachedConnection = DriverManager.getConnection(url, user, password);
            cachedConnection.setAutoCommit(true);
        }
        return cachedConnection;
    }

    public synchronized void close() {
        if (cachedConnection != null) {
            try { cachedConnection.close(); } catch (SQLException ignored) {}
            cachedConnection = null;
        }
    }
}
