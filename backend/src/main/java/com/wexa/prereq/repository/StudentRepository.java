package com.wexa.prereq.repository;

import com.wexa.prereq.model.Student;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class StudentRepository {

    private final Driver driver;

    public StudentRepository(Driver driver) {
        this.driver = driver;
    }

    public List<Student> findAll() {
        String cypher = """
                MATCH (s:Student)
                RETURN s.id AS id, s.name AS name, s.year AS year
                ORDER BY s.name
                """;

        try (Session session = driver.session()) {
            return session.run(cypher).list(this::toStudent);
        }
    }

    public List<Map<String, Object>> findCompletedCourses(String studentId) {
        String cypher = """
                MATCH (s:Student {id: $studentId})-[r:COMPLETED]->(c:Course)
                RETURN c.code AS code, c.title AS title, r.grade AS grade, r.semester AS semester
                ORDER BY r.semester, c.code
                """;

        try (Session session = driver.session()) {
            return session.run(cypher, Map.of("studentId", studentId)).list(record -> Map.of(
                    "code", record.get("code").asString(),
                    "title", record.get("title").asString(),
                    "grade", record.get("grade").asString(""),
                    "semester", record.get("semester").asString("")
            ));
        }
    }

    /** Marks a course as completed for a student. Uses MERGE so it's idempotent. */
    public void markCompleted(String studentId, String courseCode, String grade, String semester) {
        String cypher = """
                MATCH (s:Student {id: $studentId})
                MATCH (c:Course {code: $courseCode})
                MERGE (s)-[r:COMPLETED]->(c)
                SET r.grade = $grade, r.semester = $semester
                """;

        try (Session session = driver.session()) {
            session.run(cypher, Map.of(
                    "studentId", studentId,
                    "courseCode", courseCode,
                    "grade", grade == null ? "" : grade,
                    "semester", semester == null ? "" : semester
            ));
        }
    }

    private Student toStudent(Record record) {
        return new Student(
                record.get("id").asString(),
                record.get("name").asString(),
                record.get("year").asString("")
        );
    }
}
