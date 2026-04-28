package com.pipeline.infrastructure.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private final String url;
    private final String user;
    private final String password;

    public DatabaseConfig() {
        Dotenv dotenv = Dotenv.configure()
                .directory("../")
                .filename(".env")
                .load();

        this.url = dotenv.get("POSTGRES_URL");
        this.user = dotenv.get("POSTGRES_USER");
        this.password = dotenv.get("POSTGRES_PASSWORD");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public String getUrl() { return url; }
    public String getUser() { return user; }
}
