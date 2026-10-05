package com.carrental.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public final class DatabaseConnection {

    private static DatabaseConnection instance;
    private final DataSource dataSource;

    private DatabaseConnection(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static synchronized DatabaseConnection initialize(DataSource dataSource) {
        if (instance == null) {
            instance = new DatabaseConnection(dataSource);
        }
        return instance;
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseConnection has not been initialized");
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
