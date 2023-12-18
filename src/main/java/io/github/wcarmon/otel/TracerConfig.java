package io.github.wcarmon.otel;

import java.net.URI;

/** Configuration for Jaeger Tracer and Log4j2. */
public interface TracerConfig {

    /**
     * @return true when tracing is enabled
     */
    boolean enableTracing();

    /**
     * @return url for sending spans to Jaeger
     */
    URI getJaegerEndpoint();

    /**
     * @return blank if not using log4j2
     */
    String getLog4j2AppenderName();

    /**
     * @return blank unless you know why this value us useful
     */
    String getTracerName();

    /** Appears in the Service section of Jaeger UI (first dropdown) Prefixed on most spans */
    String getTracerServiceName();
}
