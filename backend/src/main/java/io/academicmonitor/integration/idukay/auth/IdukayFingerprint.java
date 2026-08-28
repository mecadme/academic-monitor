package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record IdukayFingerprint(
        @JsonProperty("user_agent") String userAgent,
        String language,
        List<String> languages,
        String platform,
        @JsonProperty("hardware_concurrency") Integer hardwareConcurrency,
        @JsonProperty("device_memory") Double deviceMemory,
        Map<String, Object> screen,
        String timezone,
        @JsonProperty("touch_points") Integer touchPoints,
        @JsonProperty("canvas_hash") String canvasHash,
        @JsonProperty("webgl_renderer") String webglRenderer,
        @JsonProperty("audio_hash") String audioHash) {

    public IdukayFingerprint {

        languages = languages == null ? List.of() : List.copyOf(languages);

        screen = screen == null ? Map.of() : Map.copyOf(screen);
    }
}
