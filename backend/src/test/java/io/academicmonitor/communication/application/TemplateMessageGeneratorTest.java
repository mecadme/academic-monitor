package io.academicmonitor.communication.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TemplateMessageGeneratorTest {

    private final TemplateMessageGenerator generator = new TemplateMessageGenerator();

    @Test
    void createsAProfessionalAcademicFollowUpWithReadableScores() {
        TemplateMessageGenerator.GeneratedMessage message =
                generator.generate("Física", "Lección TPE", new BigDecimal("1.05"), new BigDecimal("10.00"));

        assertEquals("Seguimiento académico - Física", message.subject());
        assertTrue(message.content().contains("1,05/10"));
        assertTrue(message.content().contains("Lección TPE"));
        assertTrue(message.content().contains("requiere seguimiento y acompañamiento académico"));
    }

    @Test
    void excludesInternalValuesAndUnnecessaryStudentInformation() {
        String content = generator
                .generate("Física", "Lección TPE", new BigDecimal("1.05"), new BigDecimal("10"))
                .content();

        assertFalse(content.contains("CRITICAL"));
        assertFalse(content.contains("WARNING"));
        assertFalse(content.contains("OPEN"));
        assertFalse(content.contains("RESOLVED"));
        assertFalse(content.contains("DRAFT"));
        assertFalse(content.contains("PENDING"));
        assertFalse(content.contains("SENT"));
        assertFalse(content.contains("FAILED"));
        assertFalse(content.contains("1 BGU A"));
        assertFalse(content.contains("[Su nombre]"));
    }
}
