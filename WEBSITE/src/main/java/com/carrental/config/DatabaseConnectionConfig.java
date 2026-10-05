package com.carrental.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConnectionConfig {

    @Bean
    public DatabaseConnection databaseConnection(DataSource dataSource) {
        return DatabaseConnection.initialize(dataSource);
    }
}
