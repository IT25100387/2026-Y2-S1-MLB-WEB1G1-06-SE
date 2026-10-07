package com.fuelstation.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.nio.file.*;
import java.util.Map;

/** Makes root and backend launches use the same database. */
public class DatabasePathConfig implements EnvironmentPostProcessor, Ordered {
    @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
    @Override public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path cwd=Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path projectBackend=Files.exists(cwd.resolve("backend/pom.xml")) ? cwd.resolve("backend") : cwd;
        String configured=environment.getProperty("FUELCORE_DATA_DIR");
        Path data=configured==null||configured.isBlank()?projectBackend.resolve("data"):Path.of(configured).toAbsolutePath();
        environment.getPropertySources().addLast(new MapPropertySource("fuelcoreDatabasePath",Map.of("fuelcore.database-path",data.resolve("fuelstationdb").normalize().toString().replace('\\','/'))));
    }
}
