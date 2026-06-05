package com.coralogix

/**
 * Configuration for shipping custom metrics to Coralogix via OTLP/gRPC.
 *
 * Everything is read from environment variables so the same binary can be
 * promoted across local / staging / production without rebuilding.
 *
 * Required:
 *   - CX_ENDPOINT   e.g. https://ingress.coralogix.us:443
 *   - CX_TOKEN      Coralogix "Send-Your-Data" API key
 *
 * Optional (sensible defaults provided):
 *   - CX_APPLICATION_NAME    Coralogix application label  (default: kotlin-test-application)
 *   - CX_SUBSYSTEM_NAME      Coralogix subsystem  label   (default: kotlin-test-subsystem)
 *   - CX_SERVICE_NAME        OTel service.name            (default: kotlin-otel-service)
 *   - CX_ENVIRONMENT         environment.name resource    (default: development)
 *   - CX_EXPORT_INTERVAL_MS  PeriodicMetricReader interval (default: 1000)
 */
data class CoralogixConfig(
    val endpoint: String,
    val token: String,
    val applicationName: String,
    val subsystemName: String,
    val serviceName: String,
    val environment: String,
    val exportIntervalMs: Long,
) {
    companion object {
        fun fromEnvironment(): CoralogixConfig {
            val endpoint = requireEnv("CX_ENDPOINT").let(::normalizeEndpoint)
            val token = requireEnv("CX_TOKEN")

            return CoralogixConfig(
                endpoint = endpoint,
                token = token,
                applicationName = env("CX_APPLICATION_NAME", "kotlin-test-application"),
                subsystemName = env("CX_SUBSYSTEM_NAME", "kotlin-test-subsystem"),
                serviceName = env("CX_SERVICE_NAME", "kotlin-otel-service"),
                environment = env("CX_ENVIRONMENT", "development"),
                exportIntervalMs = env("CX_EXPORT_INTERVAL_MS", "1000").toLong(),
            )
        }

        private fun requireEnv(name: String): String =
            System.getenv(name)?.takeIf { it.isNotBlank() }
                ?: error("Missing required environment variable: $name")

        private fun env(name: String, default: String): String =
            System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

        /**
         * The OTLP/gRPC exporter expects the base URL only (scheme + host + port).
         * Users sometimes paste the HTTP path (e.g. ".../v1/metrics") - strip it so
         * the exporter doesn't try to dial an invalid authority.
         */
        private fun normalizeEndpoint(raw: String): String {
            val withScheme = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
            return withScheme
                .removeSuffix("/")
                .removeSuffix("/v1/metrics")
                .removeSuffix("/")
        }
    }
}
