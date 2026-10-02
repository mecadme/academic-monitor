package io.academicmonitor.integration.idukay.guardian;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.academicmonitor.integration.idukay.course.IdukayStudentNameDto;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayParentRelationalDataDto(IdukayStudentNameDto name, String email) {}
