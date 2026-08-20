package com.wexa.prereq.controller;

import com.wexa.prereq.model.Course;
import com.wexa.prereq.model.Student;
import com.wexa.prereq.repository.CourseRepository;
import com.wexa.prereq.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public StudentController(StudentRepository studentRepository, CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    @GetMapping
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @GetMapping("/{id}/completed")
    public List<Map<String, Object>> getCompletedCourses(@PathVariable String id) {
        return studentRepository.findCompletedCourses(id);
    }

    /** The "relational-awkward" query: courses this student can take right now. */
    @GetMapping("/{id}/eligible-courses")
    public List<Course> getEligibleCourses(@PathVariable String id) {
        return courseRepository.findEligibleCourses(id);
    }

    public record CompleteCourseRequest(String grade, String semester) {}

   @PostMapping("/{id}/complete/{courseCode}")
    public ResponseEntity<Void> completeCourse(@PathVariable String id, @PathVariable String courseCode,
                                                @RequestBody(required = false) CompleteCourseRequest body) {
        String grade = body != null ? body.grade() : null;
        String semester = body != null ? body.semester() : null;
        studentRepository.markCompleted(id, courseCode, grade, semester);
        return ResponseEntity.noContent().build();
    }
}
