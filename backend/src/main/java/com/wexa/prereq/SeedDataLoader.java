package com.wexa.prereq;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Standalone seed loader - reads seed/seed.cypher, splits it into individual
 * statements, and runs them against CognoDB one at a time.
 *
 * Run it with:
 *   COGNODB_URI=bolt+s://... COGNODB_USERNAME=cognodb COGNODB_PASSWORD=... \
 *     mvn compile exec:java -Dexec.mainClass=com.wexa.prereq.SeedDataLoader
 *
 * (add the exec-maven-plugin to pom.xml, or just run this class from your IDE
 * with the env vars set - either works.)
 */
public class SeedDataLoader {

    public static void main(String[] args) throws Exception {
        String uri = requireEnv("COGNODB_URI");
        String username = System.getenv().getOrDefault("COGNODB_USERNAME", "cognodb");
        String password = requireEnv("COGNODB_PASSWORD");

        Path seedFile = Path.of("seed", "seed.cypher");
        if (!Files.exists(seedFile)) {
            // allow running from the backend/ directory too
            seedFile = Path.of("..", "seed", "seed.cypher");
        }

        String script = Files.readString(seedFile);
        List<String> statements = Arrays.stream(script.split(";"))
                .map(String::strip)
                .filter(s -> !s.isEmpty() && !s.startsWith("//"))
                .toList();

        try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password))) {
            driver.verifyConnectivity();
            System.out.println("Connected to CognoDB. Running " + statements.size() + " seed statements...");

            try (Session session = driver.session()) {
                for (String statement : statements) {
                    // strip inline comment lines within a statement, if any
                    String cleaned = statement.lines()
                            .filter(line -> !line.strip().startsWith("//"))
                            .reduce("", (a, b) -> a + "\n" + b)
                            .strip();
                    if (cleaned.isEmpty()) continue;
                    session.run(cleaned).consume();
                }
            }
            System.out.println("Seed data loaded successfully.");
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
