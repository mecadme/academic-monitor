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
                + "<p>Le informamos que se ha registrado una situación académica que requiere seguimiento.</p>"
                + "<p>Asignatura: "
                + escape(effectiveSubject)
                + "<br>Actividad: "
                + escape(effectiveActivity)
                + "<br>Calificación: "
                + score.toPlainString()
                + "/"
                + baseScore.toPlainString()
                + "</p>"
                + "<p>Solicitamos acompañar al estudiante en el proceso de mejora correspondiente.</p>"
                + "<p>Atentamente,<br>Docente</p>";
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

    public record GeneratedMessage(String subject, String content) {}
}
