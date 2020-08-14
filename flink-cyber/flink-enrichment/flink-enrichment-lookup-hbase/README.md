# HBase backed lookup table enrichment

This tool is designed to handle large scale temporal enrichments backed by HBase tables. 

The method is to provide a LookupTableSource as part of a temporal join executed in Flink SQL and collected into ordinary Message objects.