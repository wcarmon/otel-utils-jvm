package io.github.wcarmon.otel;

import io.opentelemetry.api.trace.Span;

@FunctionalInterface
public interface ConditionallyReportedSpanConsumer {

    /**
     * Like a normal java.util.function.Consumer, but handles checked exceptions.
     *
     * @param span
     * @return a decision about whether to report the span or not.
     * @throws Exception
     */
    SpanReportingDecision runIn(Span span) throws Exception;
}
