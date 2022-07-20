from os import environ

from gc import callbacks
from time import sleep
from typing import Iterable

from opentelemetry import metrics
from opentelemetry.exporter.otlp.proto.grpc.metric_exporter import OTLPMetricExporter
from opentelemetry.metrics import CallbackOptions, Observation
from opentelemetry.sdk.metrics import MeterProvider
from opentelemetry.sdk.metrics.export import PeriodicExportingMetricReader
from opentelemetry.sdk.resources import SERVICE_NAME, Resource

resource = Resource(attributes={
    SERVICE_NAME: "python-test"
})

reader = PeriodicExportingMetricReader(
    OTLPMetricExporter(
        endpoint=environ.get('CX_ENDPOINT'),
        headers=[('authorization', "Bearer " + environ.get("CX_TOKEN"))])
)

provider = MeterProvider(resource=resource, metric_readers=[reader])
metrics.set_meter_provider(provider)

meter = metrics.get_meter(__name__)

work_counter = meter.create_counter(
    "python_test_counter1", unit="", description="some counter"
)

for x in range(0, 10):
    work_counter.add(1, {"lbl1": "val1"})


def observable_gauge_func(options: CallbackOptions) -> Iterable[Observation]:
    yield Observation(0.8, {"lbl1": "val1"})

meter.create_observable_gauge(
    name="python_test_gauge1",
    description="some gauge",
    callbacks=[observable_gauge_func],
    unit="")

provider.force_flush()

sleep(3)

print("done")
