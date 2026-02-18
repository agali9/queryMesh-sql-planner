package com.dsqp.config;

import com.dsqp.execution.backend.BackendConnectionPool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class BackendConfig {

    @Bean
    public BackendConnectionPool backendConnectionPool(DsqpProperties properties) {
        Map<String, DataSource> pools = new HashMap<>();
        for (DsqpProperties.BackendConfig backend : properties.backends()) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(backend.jdbcUrl());
            config.setUsername(backend.username());
            config.setPassword(backend.password());
            config.setMaximumPoolSize(backend.poolSize());
            config.setPoolName("dsqp-" + backend.id());
            config.setConnectionTimeout(5_000);
            config.setValidationTimeout(3_000);
            pools.put(backend.id(), new HikariDataSource(config));
        }
        return new BackendConnectionPool(pools, properties.backends());
    }
}
