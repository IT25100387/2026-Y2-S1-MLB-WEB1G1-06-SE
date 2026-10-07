package com.fuelstation.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Initialize the optimistic-lock column on tanks created before versioning. */
@Component
@Order(0)
public class SchemaCompatibilityInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public SchemaCompatibilityInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        jdbc.update("update fuel_inventory set version = 0 where version is null");
    }
}
