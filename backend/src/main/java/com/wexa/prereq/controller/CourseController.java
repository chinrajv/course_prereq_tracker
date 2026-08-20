package com.wexa.prereq.controller;

import com.wexa.prereq.model.Course;
import com.wexa.prereq.repository.CourseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository courseRepository;

    public CourseController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @GetMapping("/{code}")
    public ResponseEntity<Course> getCourse(@PathVariable String code) {
        Course course = courseRepository.findByCode(code);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(course);
    }

    /** Full multi-hop prerequisite chain for a course. */
    @GetMapping("/{code}/prerequisites/chain")
    public List<Map<String, Object>> getPrerequisiteChain(@PathVariable String code) {
        return courseRepository.findPrerequisiteChain(code);
    }

    /** Direct (1-hop) prerequisites - used to render an expandable tree in the UI. */
    @GetMapping("/{code}/prerequisites/direct")
    public List<Course> getDirectPrerequisites(@PathVariable String code) {
        return courseRepository.findDirectPrerequisites(code);
    }

    /** Courses that become unlocked once this one is completed. */
    @GetMapping("/{code}/unlocks")
    public List<Course> getUnlockedCourses(@PathVariable String code) {
        return courseRepository.findCoursesUnlockedBy(code);
    }

    @GetMapping("/path")
    public List<String> getShortestPath(@RequestParam String from, @RequestParam String to) {
        return courseRepository.shortestPath(from, to);
    }
}
