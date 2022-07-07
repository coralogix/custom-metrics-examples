// Copyright The OpenTelemetry Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"context"
	"log"
	"os"
	"time"

	"crypto/tls"

	"google.golang.org/grpc/credentials"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc"
	"go.opentelemetry.io/otel/metric/global"
	"go.opentelemetry.io/otel/metric/instrument"
	controller "go.opentelemetry.io/otel/sdk/metric/controller/basic"
	processor "go.opentelemetry.io/otel/sdk/metric/processor/basic"
	"go.opentelemetry.io/otel/sdk/metric/selector/simple"
	"go.opentelemetry.io/otel/sdk/resource"
	semconv "go.opentelemetry.io/otel/semconv/v1.4.0"
)

func main() {
	ctx := context.Background()

	endpoint := os.Getenv("CX_ENDPOINT")

	headersMap := make(map[string]string)
	headersMap["Authorization"] = "Bearer " + os.Getenv("CX_TOKEN")

	metricOpts := []otlpmetricgrpc.Option{
		otlpmetricgrpc.WithTimeout(1 * time.Second),
	}
	metricOpts = append(metricOpts, otlpmetricgrpc.WithEndpoint(endpoint))
	metricOpts = append(metricOpts, otlpmetricgrpc.WithHeaders(headersMap))
	metricOpts = append(metricOpts, otlpmetricgrpc.WithTLSCredentials(credentials.NewTLS(&tls.Config{})))

	// Resource to name traces/metrics
	res0urce, err := resource.New(ctx,
		resource.WithAttributes(
			semconv.ServiceNameKey.String("go-test-service"), // job label
			semconv.ServiceVersionKey.String("v1.0.0"),
			semconv.TelemetrySDKVersionKey.String("v1.4.1"),
			semconv.TelemetrySDKLanguageGo,
		),
	)
	if err != nil {
		log.Fatalf("%s: %v", "failed to create resource", err)
	}

	exp, err := otlpmetricgrpc.New(ctx, metricOpts...)
	if err != nil {
		log.Fatalf("Failed to create the collector exporter: %v", err)
	}
	defer func() {
		ctx, cancel := context.WithTimeout(ctx, time.Second)
		defer cancel()
		if err := exp.Shutdown(ctx); err != nil {
			otel.Handle(err)
		}
	}()

	pusher := controller.New(
		processor.NewFactory(
			simple.NewWithHistogramDistribution(),
			exp,
		),
		controller.WithResource(res0urce),
		controller.WithExporter(exp),
		controller.WithCollectPeriod(1*time.Second),
	)

	global.SetMeterProvider(pusher)

	if err := pusher.Start(ctx); err != nil {
		log.Fatalf("could not start metric controller: %v", err)
	}
	defer func() {
		ctx, cancel := context.WithTimeout(ctx, time.Second)
		defer cancel()
		// pushes any last exports to the receiver
		if err := pusher.Stop(ctx); err != nil {
			otel.Handle(err)
		}
	}()

	meter := global.Meter("my-meter")

	counter, err := meter.SyncFloat64().Counter("go_otel_counter1", instrument.WithDescription("Measures the cumulative epicness of the app"))
	if err != nil {
		log.Fatalf("Failed to create the instrument: %v", err)
	}

	for i := 0; i < 10; i++ {
		log.Printf("Doing really hard work (%d / 10)\n", i+1)
		counter.Add(ctx, 1.0, attribute.String("service.name", "go-test-service"))
	}

	gaugeF, err := meter.AsyncFloat64().Gauge("go_otel_gauge1")
	if err != nil {
		log.Fatalf("Failed to create the instrument: %v", err)
	}

	err = meter.RegisterCallback([]instrument.Asynchronous{
		gaugeF,
	}, func(ctx context.Context) {
		gaugeF.Observe(ctx, float64(11), attribute.String("service.name", "go-test-service"))
	})

	pusher.Stop(ctx)
}
