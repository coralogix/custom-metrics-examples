Custom Metrics w/ python
=========================

Run the following command (make sure to fill the api key and endpoint):

```
export CX_ENDPOINT=<custom metrics endpoint>
export CX_TOKEN=<send your data api key>

pip install -r requirements.txt
python main.py
```

For the US cluster the endpoint is: https://otel-metrics.coralogix.us:443 - Visit the [Coralogix Domains](https://coralogix.com/docs/coralogix-domain/) to learn more.