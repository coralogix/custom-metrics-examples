package com.coralogix

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import java.util.concurrent.TimeUnit

/**
 * Custom Metrics → Coralogix (Kotlin)
 *
 * Mirrors the other SDK examples in this repo (java, python, nodejs, go, csharp)
 * but in idiomatic Kotlin with a tiny bit of structure so the same code is useful
 * as both a learning sample and a starting point for a real service.
 *
 * The flow:
 *   1. Read configuration from env vars  (see [CoralogixConfig])
 *   2. Build an [io.opentelemetry.sdk.metrics.SdkMeterProvider] wired to OTLP/gRPC
 *   3. Emit one of each common instrument: Counter, UpDownCounter, Histogram, ObservableGauge
 *   4. Force-flush, then shut down cleanly so nothing is dropped on exit.
 */
fun main() {
    val config = CoralogixConfig.fromEnvironment()
    printStartupBanner(config)

    val provider = CoralogixMetrics.create(config)
    try {
        val meter = provider.get("kotlin-test-meter")
        val attributes = sharedAttributes(config)

        emitCounter(meter, attributes)
        emitUpDownCounter(meter, attributes)
        emitHistogram(meter, attributes)
        emitObservableGauge(meter, attributes)

        println("Metrics recorded. Forcing flush to Coralogix...")
        provider.forceFlush().join(10, TimeUnit.SECONDS)

        // Give the periodic reader one more cycle to drain any in-flight batches.
        Thread.sleep(2_000)
        println("Done. Check Coralogix → Metrics for: kotlin_test_counter1, kotlin_test_histogram1, kotlin_test_gauge1, kotlin_test_active_sessions")
    } finally {
        provider.shutdown().join(10, TimeUnit.SECONDS)
    }
}

private fun sharedAttributes(config: CoralogixConfig): Attributes =
    Attributes.builder()
        .put("environment", config.environment)
        .put("tenant_id", "t-456")
        .put("user_id", "u-7890")
        .build()

private fun emitCounter(meter: Meter, attributes: Attributes) {
    val counter = meter.counterBuilder("kotlin_test_counter1")
        .setDescription("Number of synthetic units of work processed")
        .setUnit("1")
        .build()

    repeat(10) { counter.add(1, attributes) }
}

private fun emitUpDownCounter(meter: Meter, attributes: Attributes) {
    val active = meter.upDownCounterBuilder("kotlin_test_active_sessions")
        .setDescription("Currently active user sessions (can go up or down)")
        .setUnit("1")
        .build()

    active.add(5, attributes)
    active.add(-2, attributes)
}

private fun emitHistogram(meter: Meter, attributes: Attributes) {
    val histogram = meter.histogramBuilder("kotlin_test_histogram1")
        .setDescription("Synthetic request latency distribution")
        .setUnit("ms")
        .build()

    listOf(12.0, 45.0, 78.0, 22.0, 156.0, 89.0, 33.0).forEach { latencyMs ->
        histogram.record(latencyMs, attributes)
    }
}

private fun emitObservableGauge(meter: Meter, attributes: Attributes) {
    // Observable instruments are pulled by the SDK on each export cycle.
    // The callback is invoked by the PeriodicMetricReader, NOT by application code.
    meter.gaugeBuilder("kotlin_test_gauge1")
        .setDescription("Synthetic load factor (0.0 - 1.0)")
        .setUnit("1")
        .buildWithCallback { measurement ->
            measurement.record(0.8, attributes)
        }
}

private fun printStartupBanner(config: CoralogixConfig) {
    println("=== Coralogix Custom Metrics (Kotlin) ===")
    println("Endpoint    : ${config.endpoint}")
    println("Service     : ${config.serviceName}")
    println("Application : ${config.applicationName}")
    println("Subsystem   : ${config.subsystemName}")
    println("Environment : ${config.environment}")
    println("Interval    : ${config.exportIntervalMs} ms")
    println("==========================================")
}
