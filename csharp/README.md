
# OpenTelemetry Custom Metrics Application

This project demonstrates how to send custom metrics (Counter, Gauge, and Histogram) to an OpenTelemetry Collector using .NET.

## Prerequisites

### Install .NET SDK

#### Windows
1. Download .NET 9.0 SDK from [https://dotnet.microsoft.com/download](https://dotnet.microsoft.com/download)
2. Run the installer and follow the setup wizard

#### macOS
```bash
# Using Homebrew
brew install dotnet-sdk

# Or download from Microsoft
curl -sSL https://dot.net/v1/dotnet-install.sh | bash /dev/stdin --channel 9.0
```

#### Linux (Ubuntu/Debian)
```bash
# Add Microsoft package repository
wget https://packages.microsoft.com/config/ubuntu/22.04/packages-microsoft-prod.deb -O packages-microsoft-prod.deb
sudo dpkg -i packages-microsoft-prod.deb
rm packages-microsoft-prod.deb

# Install .NET SDK
sudo apt-get update
sudo apt-get install -y dotnet-sdk-9.0
```

Verify installation:
```bash
dotnet --version
```


## Project Structure

```
MyOpenTelemetryApp/
├── Program.cs                 # Main application entry point
├── MyOpenTelemetryApp.csproj # Project file with dependencies
└── README.md                 # This file
```


## Project Setup

### 1. Clone the repo

Clone the project
```bash
git clone https://github.com/coralogix/custom-metrics-examples.git

cd csharp

```


### 3. Export ENV variables
```bash
export CX_ENDPOINT=https://ingress.ap1.coralogix.com/v1/metrics (update your endpoint)
export CX_TOKEN=your_actual_api_key
```

### 4. Restore Dependencies
```bash
dotnet restore
```

### 5. Build Project
```bash
dotnet build
```

### 6. Run Application
```bash
dotnet run
```



## Project Setup (From scratch )

### 1. Create New Project
```bash
# Create new console application
dotnet new console -n MyOpenTelemetryApp
cd MyOpenTelemetryApp
```

### 2. Add NuGet Dependencies
```bash
# Add OpenTelemetry packages
dotnet add package OpenTelemetry --version 1.12.0
dotnet add package OpenTelemetry.Exporter.OpenTelemetryProtocol --version 1.12.0
dotnet add package OpenTelemetry.Exporter.Console --version 1.12.0
dotnet add package System.Diagnostics.DiagnosticSource --version 9.0.0
```

### 3. Update Program.cs
```csharp
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

```

### 4. Export ENV variables
```bash
export CX_ENDPOINT=https://ingress.ap1.coralogix.com/v1/metrics (update your endpoint)
export CX_TOKEN=your_actual_api_key
```


### 5. Restore Dependencies
```bash
dotnet restore
```

### 6. Build Project
```bash
dotnet build
```

### 7. Run Application
```bash
dotnet run
```

### Metrics Exported
- **Counter**: `my.counter` 
- **Gauge**: `my.gauge`
- **Histogram**: `my.histogram`


### Verify in Coralogix
- Go to Hosted Grafana and search for the metrics my_counter, my_gauge, and my_histogram to see the exported metrics.
