using System;
using System.Threading;
using System.Diagnostics.Metrics;
using OpenTelemetry;
using OpenTelemetry.Metrics;

class Program
{
    static void Main(string[] args)
    {
        using var meterProvider = Sdk.CreateMeterProviderBuilder()
            .AddMeter("MyCustomMeter")
            .AddOtlpExporter(options =>
            {
                options.Endpoint = new Uri(Environment.GetEnvironmentVariable("CX_ENDPOINT") ?? "https://ingress.ap1.coralogix.com/v1/metrics");
                options.Headers = "Authorization=Bearer " + Environment.GetEnvironmentVariable("CX_TOKEN");
                options.Protocol = OpenTelemetry.Exporter.OtlpExportProtocol.HttpProtobuf; // Use HTTP for Coralogix
            })
            .Build();
        // Create a meter
        var meter = new Meter("MyCustomMeter", "1.0.0");
        
        // Counter
        var customCounter = meter.CreateCounter<long>("my.counter", description: "A custom counter for demonstration");


        // Gauge value is observed via the callback 
        long gaugeValue = 10;
        var customGauge = meter.CreateObservableGauge<long>(
            "my.gauge",
            () => new Measurement<long>(gaugeValue, new KeyValuePair<string, object?>("tag", "value")),
            description: "A custom gauge for demonstration");

        // Histogram
        var customHistogram = meter.CreateHistogram<double>("my.histogram", description: "A custom histogram for demonstration");

        customCounter.Add(1, new KeyValuePair<string, object?>("tag", "value"));
        
        customHistogram.Record(10, new KeyValuePair<string, object?>("tag", "value"));


        Console.WriteLine("Hello, World!");
        Console.WriteLine("Current Date and Time: " + DateTime.Now.ToString());

        meterProvider.ForceFlush(5000);
        Thread.Sleep(1000);
    }
}
