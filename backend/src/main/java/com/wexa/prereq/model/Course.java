package com.wexa.prereq.model;

import java.util.List;

public class Course {
    private String code;
    private String title;
    private String department;
    private int credits;
    private String description;
    private List<Course> prerequisites; // populated only on detail/tree endpoints

    public Course() {}

    public Course(String code, String title, String department, int credits, String description) {
        this.code = code;
        this.title = title;
        this.department = department;
        this.credits = credits;
        this.description = description;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Course> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<Course> prerequisites) { this.prerequisites = prerequisites; }
}
