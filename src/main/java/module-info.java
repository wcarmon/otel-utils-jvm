/** module decl for otel */
module io.github.wcarmon.otel {
    exports io.github.wcarmon.otel;

    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.sdk.trace;
    requires io.opentelemetry.sdk;
    requires org.jetbrains.annotations;

// NOTE: uncomment if you use lombok
// requires static lombok;
}
