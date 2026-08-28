package io.academicmonitor.integration.idukay.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayTeacherCourseDto(
        @JsonProperty("_id") String id,
        String name,
        @JsonProperty("reference_name") String referenceName,
        String code,
        IdukaySubjectDto subject,
        List<IdukayStudentDto> students) {

    public IdukayTeacherCourseDto {
        students = students == null ? List.of() : List.copyOf(students);
    }
}
