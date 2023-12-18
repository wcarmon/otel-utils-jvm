package io.github.wcarmon.otel;

import io.opentelemetry.api.trace.Span;

@FunctionalInterface
public interface SpanConsumer {

    /**
     * A normal java.util.function.Consumer, but handles checked exceptions.
     *
     * @param span
     * @throws Exception
     */
    void runIn(Span span) throws Exception;
}
