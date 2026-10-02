package io.academicmonitor.integration.idukay.guardian;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.academicmonitor.integration.idukay.course.IdukayStudentRelativeDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class IdukayStudentDetailsResponseTest {

    private final JsonMapper objectMapper = new JsonMapper();

    @Test
    void deserializesRelativeParentReferenceObject() throws Exception {
        IdukayStudentDetailsResponse response = objectMapper.readValue(
                """
                {
                  "errors": [],
                  "response": [
                    {
                      "_id": "student-profile-alpha",
                      "relatives": [
                        {
                          "_id": "relationship-alpha",
                          "parent": {
                            "_id": "599f0cbd57f2723f68364df0"
                          },
                          "relationship": "Parent"
                        }
                      ]
                    }
                  ]
                }
                """,
                IdukayStudentDetailsResponse.class);

        IdukayStudentRelativeDto relative =
                response.response().getFirst().relatives().getFirst();

        assertEquals("599f0cbd57f2723f68364df0", relative.parent().id());
    }
}
