package com.spark.platform.controllers;

import com.spark.platform.models.Classroom;
import com.spark.platform.services.ClassroomService;

import java.util.List;
import java.util.Optional;

public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController() {
        this.classroomService = new ClassroomService();
    }

    public List<Classroom> getAllClassrooms() {
        return classroomService.findAll();
    }

    public Optional<Classroom> getClassroomById(int id) {
        return classroomService.findById(id);
    }

    public Classroom createClassroom(Classroom classroom) {
        return classroomService.create(classroom);
    }

    public Optional<Classroom> updateClassroom(int id, Classroom classroom) {
        classroom.setClassroomId(id);
        boolean updated = classroomService.update(classroom);
        if (!updated) {
            return Optional.empty();
        }
        return classroomService.findById(id);
    }

    public boolean deleteClassroom(int id) {
        return classroomService.delete(id);
    }
}
