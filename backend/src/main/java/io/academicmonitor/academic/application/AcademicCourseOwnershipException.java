package io.academicmonitor.academic.application;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Course not found")
public class AcademicCourseOwnershipException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public AcademicCourseOwnershipException() {
        super("Course not found");
    }
}
