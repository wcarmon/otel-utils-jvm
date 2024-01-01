package io.github.wcarmon.otel;

import java.net.URI;
import org.junit.jupiter.api.Test;

final class TracerConfigTest {

    @Test
    void conf() {
        TracerConfig.builder()
                .appenderName("foo")
                .collectorEndpoint(URI.create("http://localhost:1234"))
                .enabled(true)
                .instrumentationScopeName("aaa")
                .build();
    }
}
