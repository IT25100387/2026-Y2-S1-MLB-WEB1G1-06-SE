package com.fuelstation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class StationTimeConfig {
    @Bean
    public Clock stationClock(@Value("${app.station.time-zone:Asia/Colombo}") String zone) { return Clock.system(ZoneId.of(zone)); }
}
