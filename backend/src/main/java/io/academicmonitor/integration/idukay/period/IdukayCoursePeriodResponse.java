package io.academicmonitor.integration.idukay.period;

import tools.jackson.databind.JsonNode;

public record IdukayCoursePeriodResponse(
    JsonNode errors,
    IdukayCustomYearDto response) {}
