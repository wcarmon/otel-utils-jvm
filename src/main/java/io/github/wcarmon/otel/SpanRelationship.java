package io.github.wcarmon.otel;

public enum SpanRelationship {
    /** parent-child */
    PARENT_CHILD,

    /** follows-from, cause and effect (non-overlapping lifetimes) */
    LINKED,

    /** no parent */
    ROOT,
}
