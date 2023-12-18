package io.github.wcarmon.otel;

import io.opentelemetry.api.trace.Span;

@FunctionalInterface
public interface SpanFunction<T> {

    /**
     * A normal java.util.function.Function, but handles checked exceptions.
     *
     * @param span
     * @return T
     * @throws Exception
     */
    T runIn(Span span) throws Exception;
}
