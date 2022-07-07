Custom Metrics w/ grpcurl
=========================

Run the following command (make sure to fill the api key and endpoint):

```
export CX_ENDPOINT=<custom metrics endpoint>
export CX_TOKEN=<send your data api key>
grpcurl -v -d @  -rpc-header 'Authorization: Bearer $CX_TOKEN' $CX_ENDPOINT opentelemetry.proto.collector.metrics.v1.MetricsService/Export  < sample.json
```