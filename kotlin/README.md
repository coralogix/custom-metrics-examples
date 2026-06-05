# Custom Metrics → Coralogix (Kotlin)

A small, idiomatic Kotlin example that sends OpenTelemetry **custom metrics** to
Coralogix over OTLP/gRPC. It demonstrates the four most common instrument types:

| Instrument          | Metric name                     | Use it for                                     |
| ------------------- | ------------------------------- | ---------------------------------------------- |
| `Counter`           | `kotlin_test_counter1`          | Monotonically increasing counts (events, hits) |
| `UpDownCounter`     | `kotlin_test_active_sessions`   | Values that can go up or down (queue depth)    |
| `Histogram`         | `kotlin_test_histogram1`        | Distributions (latency, payload size)          |
| `ObservableGauge`   | `kotlin_test_gauge1`            | Point-in-time values pulled on each export     |

The project is intentionally small but split into focused files so it works as
both a **copy-paste sample** for newcomers and a **starter template** for
production services.

---

## Project layout

```
kotlin/
├── build.gradle.kts                # Gradle build with the OTel BOM pinned
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
└── src/main/kotlin/com/coralogix/
    ├── CoralogixConfig.kt          # Env-driven config (data class + parser)
    ├── CoralogixMetrics.kt         # Builds the SdkMeterProvider (reusable)
    └── Main.kt                     # Entry point – demos each instrument
```

Why the split?

- **`CoralogixConfig`** is a pure data class. Test it, mock it, inject it.
- **`CoralogixMetrics`** is the only place that knows about OTLP/gRPC, the
  Coralogix resource attributes, and the periodic reader. Drop this file into
  your own service unchanged.
- **`Main.kt`** shows _how_ to use the meter — replace it with your business
  logic.

---

## Prerequisites

- **JDK 21+** (the Gradle build pins a toolchain of 21)
- **Gradle 8.5+** _or_ use the wrapper (see [Bootstrap the Gradle wrapper](#bootstrap-the-gradle-wrapper) below)
- A Coralogix account with a **Send-Your-Data API key**

Check your tools:

```bash
java -version
gradle -v   # only if you're not using the wrapper
```

---

## Configuration

The application reads everything from environment variables. Only two are
required, the rest have sensible defaults.

| Variable                 | Required | Default                       | Purpose                                                 |
| ------------------------ | -------- | ----------------------------- | ------------------------------------------------------- |
| `CX_ENDPOINT`            | yes      | —                             | Coralogix OTLP/gRPC ingress (e.g. `https://ingress.coralogix.us:443`) |
| `CX_TOKEN`               | yes      | —                             | Send-Your-Data API key                                   |
| `CX_APPLICATION_NAME`    | no       | `kotlin-test-application`     | Coralogix `cx.application.name` label                    |
| `CX_SUBSYSTEM_NAME`      | no       | `kotlin-test-subsystem`       | Coralogix `cx.subsystem.name` label                      |
| `CX_SERVICE_NAME`        | no       | `kotlin-otel-service`         | OpenTelemetry `service.name` resource attribute          |
| `CX_ENVIRONMENT`         | no       | `development`                 | Free-form environment label                              |
| `CX_EXPORT_INTERVAL_MS`  | no       | `1000`                        | Periodic reader interval                                 |

> **Endpoint tip:** for OTLP/**gRPC** pass only the base URL — no `/v1/metrics`
> path. `CoralogixConfig` strips it for you if you paste it by mistake.

Pick your Coralogix region from the [Coralogix Domains](https://coralogix.com/docs/coralogix-domain/) docs:

| Region   | gRPC endpoint                          |
| -------- | -------------------------------------- |
| US       | `https://ingress.coralogix.us:443`     |
| US2      | `https://ingress.cx498.coralogix.com:443` |
| EU1      | `https://ingress.coralogix.com:443`    |
| EU2      | `https://ingress.eu2.coralogix.com:443` |
| AP1      | `https://ingress.coralogix.in:443`     |
| AP2      | `https://ingress.coralogixsg.com:443`  |

---

## Quick start

```bash
cd kotlin

export CX_ENDPOINT="https://ingress.coralogix.us:443"
export CX_TOKEN="<your send-your-data api key>"

gradle run
```

You should see something like:

```
=== Coralogix Custom Metrics (Kotlin) ===
Endpoint    : https://ingress.coralogix.us:443
Service     : kotlin-otel-service
Application : kotlin-test-application
Subsystem   : kotlin-test-subsystem
Environment : development
Interval    : 1000 ms
==========================================
Metrics recorded. Forcing flush to Coralogix...
Done. Check Coralogix → Metrics for: kotlin_test_counter1, ...
```

### Bootstrap the Gradle wrapper

If you'd rather not depend on system Gradle, generate the wrapper once and use
`./gradlew` going forward:

```bash
gradle wrapper --gradle-version 8.10
./gradlew run
```

### Build a runnable fat JAR

The Shadow plugin is already configured, so you can ship a single self-contained
jar:

```bash
gradle shadowJar
java -jar build/libs/otel-kotlin-sdk-example-1.0.0-all.jar
```

---

## Verify in Coralogix

1. Open Coralogix → **Custom Dashboards** / **Hosted Grafana**.
2. Pick the Coralogix metrics datasource.
3. Query any of:
   - `kotlin_test_counter1_total`
   - `kotlin_test_histogram1_bucket` (and `_count` / `_sum`)
   - `kotlin_test_gauge1`
   - `kotlin_test_active_sessions`
4. Filter by `cx_application_name="kotlin-test-application"` or
   `cx_subsystem_name="kotlin-test-subsystem"`.

> Coralogix normalizes dots in attribute keys to underscores when querying via
> PromQL (`cx.application.name` → `cx_application_name`).

---

## How the code is wired

The OpenTelemetry SDK has three core concepts. Here's where each one lives in
this project:

```
┌─────────────────────────────────┐
│  Resource                       │  ← CoralogixMetrics.buildResource()
│  (who am I? service/app/env)    │     cx.application.name, cx.subsystem.name,
│                                 │     service.name, environment.name
└────────────┬────────────────────┘
             │
┌────────────▼────────────────────┐
│  SdkMeterProvider               │  ← CoralogixMetrics.create()
│  (factory for Meters)           │     wires Resource + Reader + Exporter
└────────────┬────────────────────┘
             │
┌────────────▼────────────────────┐
│  Meter                          │  ← provider.get("kotlin-test-meter")
│  (factory for instruments)      │
└────────────┬────────────────────┘
             │
┌────────────▼────────────────────┐
│  Counter / Histogram / Gauge    │  ← Main.kt
│  (your metric calls)            │
└─────────────────────────────────┘
```

### Lifecycle, the important bit

The `PeriodicMetricReader` runs on a background thread. If the JVM exits before
it flushes, **your last metrics are lost.** This sample handles that with two
calls inside `try { } finally { }`:

```kotlin
provider.forceFlush().join(10, TimeUnit.SECONDS)  // best-effort push now
provider.shutdown().join(10, TimeUnit.SECONDS)    // stop the reader cleanly
```

In a long-running service you don't need `forceFlush()` on every operation —
the reader handles that for you on its interval. You _do_ want `shutdown()` in
a JVM shutdown hook:

```kotlin
Runtime.getRuntime().addShutdownHook(Thread {
    provider.shutdown().join(10, TimeUnit.SECONDS)
})
```

---

## Using this in your own service

Copy `CoralogixConfig.kt` and `CoralogixMetrics.kt` as-is. In your application:

```kotlin
val config = CoralogixConfig.fromEnvironment()
val provider = CoralogixMetrics.create(config)
val meter = provider.get("my-service-meter")

val requests = meter.counterBuilder("my_service_requests_total")
    .setDescription("Total HTTP requests handled")
    .setUnit("1")
    .build()

requests.add(1, Attributes.builder().put("route", "/health").build())
```

If you already use OpenTelemetry elsewhere (traces, logs, autoinstrumentation),
swap `provider.get(...)` for `GlobalOpenTelemetry.getMeter(...)` and register
the provider globally instead:

```kotlin
OpenTelemetrySdk.builder()
    .setMeterProvider(provider)
    .buildAndRegisterGlobal()
```

---

## Troubleshooting

**`UNAUTHENTICATED` / 401 / `gRPC status code 16`**
The `CX_TOKEN` is wrong or has no metrics scope. Generate a new Send-Your-Data
API key in Coralogix → Data Flow → API Keys. (A dummy token will surface as
`Failed to export metrics. Server responded with gRPC status code 16`.)

**`UNAVAILABLE` / DNS errors**
The endpoint is wrong. Double check the region table above, and remember:
- gRPC ⇒ base URL only (`https://ingress.coralogix.us:443`)
- HTTP ⇒ would include `/v1/metrics`, but this sample uses gRPC.

**Metrics never appear**
- Did the process exit before flushing? Run again and watch for the
  `Done. Check Coralogix...` line.
- Wait 30-60s after first send; new metric series can take a moment to index.
- Verify the resource labels match what you're filtering on in Coralogix.

**Enable debug logs**
```bash
JAVA_TOOL_OPTIONS="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug" gradle run
```

---

## Reference

- [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java)
- [OTLP exporter docs](https://opentelemetry.io/docs/languages/java/exporters/#otlp)
- [Coralogix custom metrics](https://coralogix.com/docs/integrations/coralogix-endpoints/)
- [Coralogix domains / regions](https://coralogix.com/docs/coralogix-domain/)
