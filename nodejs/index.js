const { OTLPMetricExporter } = require('@opentelemetry/exporter-metrics-otlp-grpc');
const { MeterProvider, PeriodicExportingMetricReader } = require('@opentelemetry/sdk-metrics');
const { Resource } = require('@opentelemetry/resources');
const { SemanticResourceAttributes } = require('@opentelemetry/semantic-conventions');
const { Metadata } = require('@grpc/grpc-js');

const metadata = new Metadata();
metadata.add('Authorization', 'Bearer ' + process.env.CX_TOKEN);

const collectorOptions = {
    url: process.env.CX_ENDPOINT,
    metadata: metadata
};

const metricExporter = new OTLPMetricExporter(collectorOptions);

const meterProvider = new MeterProvider({
    resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'basic-metric-service',
    }),
});

meterProvider.addMetricReader(new PeriodicExportingMetricReader({
    exporter: metricExporter,
    exportIntervalMillis: 100,
}));

['SIGINT', 'SIGTERM'].forEach(signal => {
    process.on(signal, () => meterProvider.shutdown().catch(console.error));
});

const meter = meterProvider.getMeter('example-exporter-collector');

const requestCounter = meter.createCounter('nodejs_test_counter1', {
    description: 'Example of a Counter',
});

const attributes = { pid: process.pid, environment: 'staging', 'lbl1': 'val1' };

requestCounter.add(1, attributes);
requestCounter.add(1, attributes);

sleep(200);

console.debug("done!");

function sleep(ms) {
    return new Promise((resolve) => {
        setTimeout(resolve, ms);
    });
}
