package io.github.wcarmon.otel;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import org.jetbrains.annotations.Nullable;

/** Configuration for Jaeger Tracer and Log4j2. */
public record TracerConfig(
        boolean enabled,

        /* url for (Jaeger) collector, required when tracing enabled */
        @Nullable URI collectorEndpoint,

        /* name of the appender, or blank if not sending span events to log4j2/logback */
        String appenderName,

        /*
         * leave blank, unless you know why this value us useful
         * See https://opentelemetry.io/docs/specs/otel/trace/api/#get-a-tracer
         */
        String instrumentationScopeName,

        /* Appears in the Service section of Jaeger UI (first dropdown) Prefixed on most spans */
        String tracerServiceName) {

    public TracerConfig {
        if (enabled) {
            requireNonNull(collectorEndpoint, "collectorEndpoint is required and null.");
        }

        if (instrumentationScopeName == null || instrumentationScopeName.isBlank()) {
            instrumentationScopeName = "";
        }
        instrumentationScopeName = instrumentationScopeName.strip();
        // TODO: Precondition for max length, acceptable chars

        if (appenderName == null || appenderName.isBlank()) {
            appenderName = "";
        }
        appenderName = appenderName.strip();
        // TODO: Precondition for max length, acceptable chars

        if (tracerServiceName == null || tracerServiceName.isBlank()) {
            tracerServiceName = "";
        }
        tracerServiceName = tracerServiceName.strip();
        // TODO: Precondition for max length, acceptable chars
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .enabled(this.enabled)
                .collectorEndpoint(this.collectorEndpoint)
                .appenderName(this.appenderName)
                .instrumentationScopeName(this.instrumentationScopeName)
                .tracerServiceName(this.tracerServiceName);
    }

    public static class Builder {

        private String appenderName;
        private @Nullable URI collectorEndpoint;
        private boolean enabled;
        private String instrumentationScopeName;
        private String tracerServiceName;

        Builder() {}

        public Builder appenderName(String appenderName) {
            this.appenderName = appenderName;
            return this;
        }

        public TracerConfig build() {
            return new TracerConfig(
                    this.enabled,
                    this.collectorEndpoint,
                    this.appenderName,
                    this.instrumentationScopeName,
                    this.tracerServiceName);
        }

        public Builder collectorEndpoint(@Nullable URI collectorEndpoint) {
            this.collectorEndpoint = collectorEndpoint;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder instrumentationScopeName(String instrumentationScopeName) {
            this.instrumentationScopeName = instrumentationScopeName;
            return this;
        }

        public String toString() {
            return "TracerConfig.Builder(enabled="
                    + this.enabled
                    + ", collectorEndpoint="
                    + this.collectorEndpoint
                    + ", appenderName="
                    + this.appenderName
                    + ", instrumentationScopeName="
                    + this.instrumentationScopeName
                    + ", tracerServiceName="
                    + this.tracerServiceName
                    + ")";
        }

        public Builder tracerServiceName(String tracerServiceName) {
            this.tracerServiceName = tracerServiceName;
            return this;
        }
    }
}
