package com.fuelstation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FuelStationApplication {

    public static void main(String[] args) {
        // Use explicit restarts so compiling cannot open a second H2 connection.
        System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(FuelStationApplication.class, args);
    }
}
