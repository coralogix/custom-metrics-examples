package com.coralogix

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes
import java.time.Duration

/**
 * Builds a fully-configured [SdkMeterProvider] that exports OTLP/gRPC metrics
 * to Coralogix.
 *
 * The provider attaches the Coralogix-specific resource attributes that the
 * platform uses to route and label metrics:
 *   - cx.application.name
 *   - cx.subsystem.name
 *   - environment.name
 *
 * It also sets the standard OpenTelemetry [ServiceAttributes.SERVICE_NAME].
 *
 * The returned provider owns a [PeriodicMetricReader] background thread, so
 * callers MUST invoke [SdkMeterProvider.shutdown] before the JVM exits.
 */
object CoralogixMetrics {

    fun create(config: CoralogixConfig): SdkMeterProvider {
        val resource = buildResource(config)
        val exporter = buildExporter(config)

        val reader = PeriodicMetricReader.builder(exporter)
            .setInterval(Duration.ofMillis(config.exportIntervalMs))
            .build()

        return SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(reader)
            .build()
    }

    private fun buildResource(config: CoralogixConfig): Resource {
        val coralogixAttrs = Attributes.builder()
            .put(ServiceAttributes.SERVICE_NAME, config.serviceName)
            .put("cx.application.name", config.applicationName)
            .put("cx.subsystem.name", config.subsystemName)
            .put("environment.name", config.environment)
            .build()

        return Resource.getDefault().merge(Resource.create(coralogixAttrs))
    }

    private fun buildExporter(config: CoralogixConfig): OtlpGrpcMetricExporter =
        OtlpGrpcMetricExporter.builder()
            .setEndpoint(config.endpoint)
            .addHeader("Authorization", "Bearer ${config.token}")
            .setTimeout(Duration.ofSeconds(10))
            .build()
}
