package com.wexa.prereq.repository;

import com.wexa.prereq.model.Course;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * All queries here use parameters ($code, $studentId, etc.) rather than
 * string concatenation, to avoid Cypher injection - same principle as
 * parameterized SQL.
 */
@Repository
public class CourseRepository {

    private final Driver driver;

    public CourseRepository(Driver driver) {
        this.driver = driver;
    }

    /** Simple flat list of all courses, for the home page. */
    public List<Course> findAll() {
        String cypher = """
                MATCH (c:Course)
                RETURN c.code AS code, c.title AS title, c.department AS department,
                       c.credits AS credits, c.description AS description
                ORDER BY c.department, c.code
                """;

        try (Session session = driver.session()) {
            return session.run(cypher).list(this::toCourse);
        }
    }

    /** Single course by code, no prerequisites attached. */
    public Course findByCode(String code) {
        String cypher = """
                MATCH (c:Course {code: $code})
                RETURN c.code AS code, c.title AS title, c.department AS department,
                       c.credits AS credits, c.description AS description
                """;

        try (Session session = driver.session()) {
            return session.run(cypher, Map.of("code", code))
                    .list(this::toCourse)
                    .stream().findFirst().orElse(null);
        }
    }

    /**
     * MULTI-HOP TRAVERSAL (2+ hops): the full prerequisite chain for a course,
     * following REQUIRES edges as deep as they go (capped at 6 hops to keep
     * it bounded). This is the query a relational schema would need a
     * recursive CTE for; here it's one pattern match.
     */
    public List<Map<String, Object>> findPrerequisiteChain(String code) {
        String cypher = """
                MATCH path = (target:Course {code: $code})-[:REQUIRES*1..6]->(prereq:Course)
                WITH prereq, min(length(path)) AS depth
                RETURN prereq.code AS code, prereq.title AS title,
                       prereq.department AS department, depth
                ORDER BY depth, prereq.code
                """;

        try (Session session = driver.session()) {
            return session.run(cypher, Map.of("code", code)).list(record -> Map.of(
                    "code", record.get("code").asString(),
                    "title", record.get("title").asString(),
                    "department", record.get("department").asString(),
                    "depth", record.get("depth").asInt()
            ));
        }
    }

    /** Direct (1-hop) prerequisites only - used to render an immediate-parents tree node by node. */
    public List<Course> findDirectPrerequisites(String code) {
        String cypher = """
                MATCH (c:Course {code: $code})-[:REQUIRES]->(prereq:Course)
                RETURN prereq.code AS code, prereq.title AS title, prereq.department AS department,
                       prereq.credits AS credits, prereq.description AS description
                ORDER BY prereq.code
                """;

        try (Session session = driver.session()) {
            return session.run(cypher, Map.of("code", code)).list(this::toCourse);
        }
    }

    /** Reverse traversal: which courses get unlocked once this one is completed. */
    public List<Course> findCoursesUnlockedBy(String code) {
        String cypher = """
                MATCH (c:Course {code: $code})<-[:REQUIRES]-(dependent:Course)
                RETURN dependent.code AS code, dependent.title AS title, dependent.department AS department,
                       dependent.credits AS credits, dependent.description AS description
                ORDER BY dependent.code
                """;

        try (Session session = driver.session()) {
            return session.run(cypher, Map.of("code", code)).list(this::toCourse);
        }
    }

    /** Shortest prerequisite path between two arbitrary courses. */
    public List<String> shortestPath(String fromCode, String toCode) {
        String cypher = """
                MATCH path = shortestPath(
                    (a:Course {code: $from})-[:REQUIRES*]-(b:Course {code: $to})
                )
                RETURN [n IN nodes(path) | n.code] AS codes
                """;

        try (Session session = driver.session()) {
            var result = session.run(cypher, Map.of("from", fromCode, "to", toCode));
            if (!result.hasNext()) return List.of();
            return result.single().get("codes").asList(Values.ofString());
        }
    }

    /**
     * RELATIONAL-AWKWARD QUERY: courses a given student is eligible to take
     * right now - every prerequisite must be satisfied AND the course must
     * not already be completed. In SQL this needs a NOT EXISTS correlated
     * subquery per-course-per-prerequisite (or a HAVING count-matches trick);
     * in Cypher it's a direct pattern-negation.
     */
    public List<Course> findEligibleCourses(String studentId) {
        String cypher = """
                MATCH (c:Course)
                WHERE NOT EXISTS {
                    MATCH (s:Student {id: $studentId})-[:COMPLETED]->(c)
                }
                AND NOT EXISTS {
                    MATCH (c)-[:REQUIRES]->(req:Course)
                    WHERE NOT EXISTS {
                        MATCH (s2:Student {id: $studentId})-[:COMPLETED]->(req)
                    }
                }
                RETURN c.code AS code, c.title AS title, c.department AS department,
                       c.credits AS credits, c.description AS description
                ORDER BY c.department, c.code
                """;

        try (Session session = driver.session()) {
            return session.run(cypher, Map.of("studentId", studentId)).list(this::toCourse);
        }
    }

    private Course toCourse(Record record) {
        return new Course(
                record.get("code").asString(),
                record.get("title").asString(),
                record.get("department").asString(),
                record.get("credits").asInt(0),
                record.get("description").asString("")
        );
    }
}
