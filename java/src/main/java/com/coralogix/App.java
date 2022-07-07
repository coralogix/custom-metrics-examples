package com.coralogix;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

public class App 
{
  public static void main( String[] args ) throws Exception
  {
    String endpoint = System.getenv("CX_ENDPOINT");
    if (!endpoint.startsWith("https://")) {
      endpoint = "https://" + endpoint;
    }

    String token = System.getenv("CX_TOKEN");

    SdkMeterProvider meterProvider = 
      SdkMeterProvider.builder()
        .registerMetricReader(
          PeriodicMetricReader.builder(
            OtlpGrpcMetricExporter.builder()
              .setEndpoint(endpoint)
              .addHeader("Authorization", "Bearer " + token)
          .build())
        .build())
      .build();
          
          
    Meter meter = meterProvider.meterBuilder("test").build();
    
    LongCounter counter = meter
      .counterBuilder("java_test_counter1")
      .setDescription("Processed jobs")
      .build();

    counter.add(
      100l, 
      Attributes.of(AttributeKey.stringKey("service.name"), "my-test-service")
    );
    
    meter
      .gaugeBuilder("java_test_gauge1")
      .buildWithCallback(measurement -> {
        measurement.record(0.8, Attributes.of(AttributeKey.stringKey("service.name"), "my-test-service"));
      });

    meterProvider.forceFlush();

    Thread.sleep(3000l);

    System.out.println("done");
  }
}
