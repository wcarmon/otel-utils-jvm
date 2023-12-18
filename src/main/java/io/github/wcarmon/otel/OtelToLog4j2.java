package io.github.wcarmon.otel;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.StringMapMessage;
import org.jetbrains.annotations.Nullable;

/** Converts SpanData to Log4j2 LogEvent. */
public final class OtelToLog4j2 {

    private final Level defaultLevel;

    private final String loggerName;

    private OtelToLog4j2(String loggerName, @Nullable Level defaultLevel) {

        if (loggerName == null || loggerName.isBlank()) {
            throw new IllegalArgumentException("loggerName is required");
        }

        this.defaultLevel = Objects.requireNonNullElse(defaultLevel, Level.INFO);
        this.loggerName = loggerName;
    }

    public static OtelToLog4j2Builder builder() {
        return new OtelToLog4j2Builder();
    }

    public static void main(String... args) {
        System.out.println("hi");
    }

    public LogEvent convertEvent(EventData spanEvent, SpanData spanData) {
        requireNonNull(spanEvent, "spanEvent is required and null.");

        final var level = getLevel(spanEvent, spanData);

        final var message = new StringMapMessage();
        copyAttributes(spanData, message);
        message.put("message", spanEvent.getName());

        if ("exception".equals(spanEvent.getName())) {

            // TODO: improve me
            // spanEvent.exception has the exception, but it's private
            // spanData.getEvents().get(0).exception also has the exception, but it's private
            System.err.println(
                    "TODO: figure out how to get the exception from the spanData or spanEvent");

            //            message.put("exception", ??);
        }

        return Log4jLogEvent.newBuilder()
                .setIncludeLocation(false)
                .setLevel(level)
                .setLoggerFqcn(OtelToLog4j2.class.getName())
                .setLoggerName(loggerName)
                .setMessage(message)
                .setNanoTime(spanEvent.getEpochNanos())
                .build();
    }

    public List<LogEvent> convertEvents(SpanData spanData) {
        requireNonNull(spanData, "spanData is required and null.");

        if (spanData.getEvents().isEmpty()) {
            throw new IllegalArgumentException("at least one event required");
        }

        return spanData.getEvents().stream()
                .map(spanEvent -> convertEvent(spanEvent, spanData))
                .collect(Collectors.toList());
    }

    private void copyAttributes(SpanData spanData, StringMapMessage message) {
        requireNonNull(message, "message is required and null.");
        requireNonNull(spanData, "spanData is required and null.");

        spanData.getAttributes()
                .forEach((key, value) -> message.put(key.getKey(), String.valueOf(value)));
    }

    private Level getLevel(EventData spanEvent, SpanData spanData) {
        requireNonNull(spanData, "spanData is required and null.");
        requireNonNull(spanEvent, "spanEvent is required and null.");

        if (spanData.getStatus() == StatusData.error()) {
            return Level.ERROR;
        }

        final var rawLevel = getLevelAttributeValue(spanEvent.getAttributes());
        final var desiredLevel = parseLevel(rawLevel);

        if (desiredLevel != null) {
            return desiredLevel;
        }

        return defaultLevel;
    }

    private String getLevelAttributeValue(Attributes attributes) {
        requireNonNull(attributes, "attributes is required and null.");

        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
            var k = String.valueOf(entry.getKey());
            if ("level".equalsIgnoreCase(k)) {
                return String.valueOf(entry.getValue());
            }
        }

        return "";
    }

    /**
     * handles null, blank, trims, normalizes case
     *
     * @param level "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL" (any case, whitespace
     *     ignored)
     * @return null only when cannot parse Log4j2 level
     */
    @Nullable
    private Level parseLevel(String level) {

        final var clean = level == null ? "" : level.strip().toUpperCase();

        if (clean.isBlank()) {
            return null;
        }

        return Level.getLevel(clean);
    }

    public static class OtelToLog4j2Builder {

        private @Nullable Level defaultLevel;
        private String loggerName;

        OtelToLog4j2Builder() {}

        public OtelToLog4j2 build() {
            return new OtelToLog4j2(this.loggerName, this.defaultLevel);
        }

        public OtelToLog4j2Builder defaultLevel(@Nullable Level defaultLevel) {
            this.defaultLevel = defaultLevel;
            return this;
        }

        public OtelToLog4j2Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public String toString() {
            return "OtelToLog4j2.OtelToLog4j2Builder(loggerName="
                    + this.loggerName
                    + ", defaultLevel="
                    + this.defaultLevel
                    + ")";
        }
    }
}
