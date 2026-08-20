package com.wexa.prereq.model;

public class Student {
    private String id;
    private String name;
    private String year;

    public Student() {}

    public Student(String id, String name, String year) {
        this.id = id;
        this.name = name;
        this.year = year;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
}
