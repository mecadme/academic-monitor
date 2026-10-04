package io.academicmonitor.communication.application;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** A deliberately deterministic, provider-neutral initial message template. */
@Component
public class TemplateMessageGenerator {

    public GeneratedMessage generate(String subjectName, String activityName, BigDecimal score, BigDecimal baseScore) {
        String effectiveSubject = nonBlank(subjectName, "Seguimiento académico");
        String effectiveActivity = nonBlank(activityName, "Actividad académica");
        String subject = "Seguimiento académico - " + effectiveSubject;
        String content = "<p>Estimado/a representante:</p>"
                + "<p>En la asignatura de "
                + escape(effectiveSubject)
                + ", el/la estudiante obtuvo una calificación de "
                + formatScore(score)
                + "/"
                + formatScore(baseScore)
                + " en la actividad «"
                + escape(effectiveActivity)
                + "». Este resultado requiere seguimiento y acompañamiento académico.</p>"
                + "<p>Se recomienda revisar los contenidos trabajados y apoyar el proceso de refuerzo para "
                + "favorecer una mejora en próximas actividades.</p>"
                + "<p>Agradecemos su atención y acompañamiento en el proceso académico.</p>";
        return new GeneratedMessage(subject, content);
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String escape(String value) {
        return Objects.requireNonNull(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String formatScore(BigDecimal value) {
        BigDecimal normalized = (value == null ? BigDecimal.ZERO : value).stripTrailingZeros();
        if (normalized.scale() < 0) normalized = normalized.setScale(0);
        return normalized.toPlainString().replace('.', ',');
    }

    public record GeneratedMessage(String subject, String content) {}
}
