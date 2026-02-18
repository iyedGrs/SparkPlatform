package com.spark.platform.controllers;

import com.spark.platform.models.Course;
import com.spark.platform.services.CourseService;

import java.util.List;
import java.util.Optional;

public class CourseController {

    private final CourseService courseService;

    public CourseController() {
        this.courseService = new CourseService();
    }

    public List<Course> getAllCourses() {
        return courseService.findAll();
    }

    public Optional<Course> getCourseById(int id) {
        return courseService.findById(id);
    }

    public Course createCourse(Course course) {
        return courseService.create(course);
    }

    public Optional<Course> updateCourse(int id, Course course) {
        course.setCourseId(id);
        boolean updated = courseService.update(course);
        if (!updated) {
            return Optional.empty();
        }
        return courseService.findById(id);
    }

    public boolean deleteCourse(int id) {
        return courseService.delete(id);
    }
}
