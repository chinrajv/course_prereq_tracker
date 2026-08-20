package com.wexa.prereq.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PreDestroy;

import java.time.Duration;

/**
 * Creates a single shared Neo4j driver instance pointed at CognoDB.
 * CognoDB speaks the Bolt protocol and is compatible with the official
 * Neo4j driver, so no custom SDK is needed - just point it at the
 * bolt+s:// URI with the "cognodb" user and the generated password.
 *
 * Credentials are read from environment variables (see application.yml)
 * and are never hardcoded or committed to the repo.
 */
@Configuration
public class Neo4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Neo4jConfig.class);

    private Driver driver;

    @Bean
    public Driver neo4jDriver(
            @Value("${cognodb.uri}") String uri,
            @Value("${cognodb.username}") String username,
            @Value("${cognodb.password}") String password) {

        if (password == null || password.isBlank()) {
            log.warn("COGNODB_PASSWORD is not set. The application will start, " +
                    "but every database call will fail until credentials are configured.");
        }

        this.driver = GraphDatabase.driver(
                uri,
                AuthTokens.basic(username, password),
                Config.builder()
                        .withConnectionTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .withMaxConnectionPoolSize(20)
                        .build()
        );

        // Verify connectivity at startup but don't crash the app if CognoDB
        // happens to be unreachable right now - requests will surface a
        // clean error instead (see GlobalExceptionHandler).
        try {
            driver.verifyConnectivity();
            log.info("Connected to CognoDB successfully.");
        } catch (Exception e) {
            log.error("Could not connect to CognoDB at startup: {}", e.getMessage());
        }

        return driver;
    }

    @PreDestroy
    public void closeDriver() {
        if (driver != null) {
            driver.close();
        }
    }
}
