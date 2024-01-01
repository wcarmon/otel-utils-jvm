package io.github.wcarmon.otel;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.jetbrains.annotations.Nullable;

// TODO: publish this

/** Simplify idiomatic OpenTelemetry usage */
public final class SpanUtils {

    private SpanUtils() {}

    /**
     * Use inside a catch block
     *
     * @param span
     * @param ex
     */
    public static RuntimeException record(Span span, Throwable ex) {
        requireNonNull(span, "span is required and null.");
        requireNonNull(ex, "ex is required and null.");

        span.recordException(ex);
        span.setStatus(StatusCode.ERROR);

        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        }

        // -- wrap
        return new RuntimeException(ex);
    }

    /**
     * Creates new root span (no parent, no links)
     *
     * <p>Never relies on thread-local
     *
     * @param tracer
     * @param spanName
     * @param spanConsumer
     */
    public static void runInARootSpan(Tracer tracer, String spanName, SpanConsumer spanConsumer) {
        requireSpanName(spanName);
        requireNonNull(spanConsumer, "spanConsumer is required and null.");
        requireNonNull(tracer, "tracer is required and null.");

        final var span = tracer.spanBuilder(spanName).setNoParent().startSpan();
        doRun(span, spanConsumer);
    }

    /**
     * Parent-child relationship
     *
     * <p>Never relies on thread-local
     *
     * @param tracer
     * @param spanName
     * @param parent
     * @param spanConsumer
     */
    public static void runInChildSpan(
            Tracer tracer, String spanName, SpanContext parent, SpanConsumer spanConsumer) {

        requireSpanName(spanName);
        requireNonNull(parent, "parent is required and null.");
        requireNonNull(spanConsumer, "spanConsumer is required and null.");
        requireNonNull(tracer, "tracer is required and null.");

        final var span =
                tracer.spanBuilder(spanName)
                        .setParent(Context.current().with(Span.wrap(parent)))
                        .startSpan();
        doRun(span, spanConsumer);
    }

    /**
     * Follows-from relationship
     *
     * <p>Never relies on thread-local
     *
     * <p>Jaeger treats linked span like a root span with a hyperlinked reference property
     *
     * @param tracer
     * @param spanName
     * @param cause
     * @param spanConsumer
     */
    public static void runInLinkedSpan(
            Tracer tracer, String spanName, SpanContext cause, SpanConsumer spanConsumer) {

        requireSpanName(spanName);
        requireNonNull(cause, "cause is required and null.");
        requireNonNull(spanConsumer, "spanConsumer is required and null.");
        requireNonNull(tracer, "tracer is required and null.");

        final var span = tracer.spanBuilder(spanName).setNoParent().addLink(cause).startSpan();
        doRun(span, spanConsumer);
    }

    /**
     * A generalization of runInARootSpan, runInChildSpan, and runInLinkedSpan
     *
     * <p>with conditional span reporting
     *
     * @param tracer
     * @param spanName
     * @param relationship
     * @param parentOrCause
     * @param spanConsumer
     */
    public static void runInSpan(
            Tracer tracer,
            String spanName,
            SpanRelationship relationship,
            @Nullable SpanContext parentOrCause,
            ConditionallyReportedSpanConsumer spanConsumer) {

        requireSpanName(spanName);
        requireNonNull(relationship, "relationship is required and null.");
        requireNonNull(spanConsumer, "spanConsumer is required and null.");
        requireNonNull(tracer, "tracer is required and null.");

        final var span =
                switch (relationship) {
                    case PARENT_CHILD -> {
                        requireNonNull(parentOrCause, "parent is required and null.");

                        yield tracer.spanBuilder(spanName)
                                .setParent(Context.current().with(Span.wrap(parentOrCause)))
                                .startSpan();
                    }

                    case LINKED -> {
                        requireNonNull(parentOrCause, "cause is required and null.");

                        yield tracer.spanBuilder(spanName)
                                .setNoParent()
                                .addLink(parentOrCause)
                                .startSpan();
                    }

                    case ROOT -> {
                        if (parentOrCause != null) {
                            throw new IllegalArgumentException(
                                    "parent/cause must be null for root span");
                        }

                        yield tracer.spanBuilder(spanName).setNoParent().startSpan();
                    }
                };

        try (var ignored = span.makeCurrent()) {
            // span.setAttribute(SemanticAttributes.THREAD_ID,
            //          Thread.currentThread().threadId()); // Only Java 19+

            span.setAttribute(
                    // TODO: this is broken somehow
                    // SemanticAttributes.THREAD_ID,
                    "thread.id", Thread.currentThread().getId());
            span.setAttribute(
                    // TODO: this is broken somehow
                    // SemanticAttributes.THREAD_NAME,
                    "thread.name", Thread.currentThread().getName());

            final var shouldReport = spanConsumer.runIn(span);

            // -- Only report span if requested
            if (shouldReport == SpanReportingDecision.REPORT) {
                span.end();
                return;
            }

            if (shouldReport == null) {
                throw new IllegalStateException(
                        "Programming error: must return span reporting decision");
            }

        } catch (Exception ex) {
            record(span, ex);

            // -- Always report on failure
            span.end();
            throw new RuntimeException(ex);
        }
    }

    /**
     * Creates new root span (no parent, no links)
     *
     * <p>Never relies on thread-local
     *
     * @param tracer
     * @param spanName
     * @param fn
     * @return whatever the fn returns
     */
    public static <T> T supplyInARootSpan(Tracer tracer, String spanName, SpanFunction<T> fn) {
        requireSpanName(spanName);
        requireNonNull(fn, "fn is required and null.");
        requireNonNull(tracer, "tracer is required and null.");

        final var span = tracer.spanBuilder(spanName).setNoParent().startSpan();
        return doSupply(span, fn);
    }

    /**
     * Parent-child relationship
     *
     * <p>Never relies on thread-local
     *
     * @param tracer
     * @param spanName
     * @param parent
     * @param fn
     * @return whatever the fn returns
     */
    public static <T> T supplyInChildSpan(
            Tracer tracer, String spanName, SpanContext parent, SpanFunction<T> fn) {

        requireSpanName(spanName);
        requireNonNull(fn, "fn is required and null.");
        requireNonNull(parent, "parent is required and null.");
        requireNonNull(tracer, "tracer is required and null.");

        final var span =
                tracer.spanBuilder(spanName)
                        .setParent(Context.current().with(Span.wrap(parent)))
                        .startSpan();
        return doSupply(span, fn);
    }

    /**
     * Follows-from relationship
     *
     * <p>Never relies on thread-local
     *
     * <p>Jaeger treats linked span like a root span with a hyperlinked reference property
     *
     * @param tracer
     * @param spanName
     * @param cause
     * @param fn
     * @return whatever the fn returns
     */
    public static <T> T supplyInLinkedSpan(
            Tracer tracer, String spanName, SpanContext cause, SpanFunction<T> fn) {

        requireSpanName(spanName);
        requireNonNull(fn, "fn is required and null.");
        requireNonNull(cause, "cause is required and null.");
        requireNonNull(tracer, "tracer is required and null.");

        final var span = tracer.spanBuilder(spanName).setNoParent().addLink(cause).startSpan();
        return doSupply(span, fn);
    }

    private static void doRun(Span span, SpanConsumer spanConsumer) {
        try (var ignored = span.makeCurrent()) {
            span.setAttribute("thread.id", Thread.currentThread().getId()); // TODO: use threadId()
            span.setAttribute("thread.name", Thread.currentThread().getName());

            spanConsumer.runIn(span);

        } catch (Exception ex) {
            System.err.println("unhandled exception: " + ex);
            throw record(span, ex);

        } finally {
            span.end();
        }
    }

    private static <T> T doSupply(Span span, SpanFunction<T> fn) {
        try (var ignored = span.makeCurrent()) {
            span.setAttribute("thread.id", Thread.currentThread().getId()); // TODO: use threadId()
            span.setAttribute("thread.name", Thread.currentThread().getName());

            return fn.runIn(span);

        } catch (Exception ex) {
            throw record(span, ex);

        } finally {
            span.end();
        }
    }

    private static void requireSpanName(String spanName) {
        if (spanName == null || spanName.isBlank()) {
            throw new IllegalArgumentException("spanName is required");
        }
    }
}
