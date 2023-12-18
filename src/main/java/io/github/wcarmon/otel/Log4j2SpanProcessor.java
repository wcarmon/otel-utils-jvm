package io.github.wcarmon.otel;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;

/** Forwards all Span events to a Log4j2 appender. */
public final class Log4j2SpanProcessor implements SpanProcessor {

    private final Appender appender;

    private final OtelToLog4j2 converter;

    private Log4j2SpanProcessor(OtelToLog4j2 converter, String targetAppenderName) {

        requireNonNull(converter, "converter is required and null.");
        if (targetAppenderName == null || targetAppenderName.isBlank()) {
            throw new IllegalArgumentException("targetAppenderName is required");
        }

        this.converter = converter;

        final var context = (LoggerContext) LogManager.getContext(false);
        final var config = context.getConfiguration();
        appender = config.getAppender(targetAppenderName);

        if (appender == null) {
            throw new IllegalStateException(
                    "failed to lookup appender: name=" + targetAppenderName);
        }
    }

    public static Log4j2SpanProcessorBuilder builder() {
        return new Log4j2SpanProcessorBuilder();
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {

        final var spanData = span.toSpanData();
        final var events = spanData.getEvents();
        if (events == null || events.isEmpty()) {
            // -- Nothing to log
            return;
        }

        converter.convertEvents(spanData).forEach(appender::append);
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {}

    public static class Log4j2SpanProcessorBuilder {

        private OtelToLog4j2 converter;
        private String targetAppenderName;

        Log4j2SpanProcessorBuilder() {}

        public Log4j2SpanProcessor build() {
            return new Log4j2SpanProcessor(this.converter, this.targetAppenderName);
        }

        public Log4j2SpanProcessorBuilder converter(OtelToLog4j2 converter) {
            this.converter = converter;
            return this;
        }

        public Log4j2SpanProcessorBuilder targetAppenderName(String targetAppenderName) {
            this.targetAppenderName = targetAppenderName;
            return this;
        }

        public String toString() {
            return "Log4j2SpanProcessor.Log4j2SpanProcessorBuilder(converter="
                    + this.converter
                    + ", targetAppenderName="
                    + this.targetAppenderName
                    + ")";
        }
    }
}
