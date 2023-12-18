package io.github.wcarmon.otel;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.List;

/**
 * Typical patterns for constructing OpenTelemetry classes
 *
 * <p>Useful with or without dependency injection
 */
public final class OTelFactories {

    /**
     * Sets up the if desired, register output globally in main:
     * GlobalOpenTelemetry.set(openTelemetry);
     *
     * @return properly configured OpenTelemetry instance
     */
    public static OpenTelemetry buildOpenTelemetry(
            String serviceName, List<SpanProcessor> spanProcessors) {

        requireNonNull(spanProcessors, "spanProcessors is required and null.");
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }

        final var attrs =
                Attributes.builder()
                        .put("service.name", serviceName)

                        // -- See opentelemetry-semconv-x.y.z-alpha.jar
                        // -- Inlining the string avoids yet another dependency
                        // -- That jar also lacks java module support
                        // Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)

                        .build();

        final var serviceNameResource = Resource.create(attrs);
        final var resource = Resource.getDefault().merge(serviceNameResource);

        final var b = SdkTracerProvider.builder();
        spanProcessors.forEach(b::addSpanProcessor);

        final var tracerProvider = b.setResource(resource).build();

        final var openTelemetry =
                OpenTelemetrySdk.builder()
                        .setPropagators(
                                ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                        .setTracerProvider(tracerProvider)
                        .build();

        // -- Push final spans on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        return openTelemetry;
    }

    public static Tracer tracer(OpenTelemetry openTelemetry) {
        requireNonNull(openTelemetry, "openTelemetry is required and null.");

        // -- unnamed tracer by default since most applications don't need multiple
        return openTelemetry.getTracer("");
    }
}
