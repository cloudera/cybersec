#!/bin/sh

solrctl collection --delete dpi_dns
solrctl collection --delete dpi_http
solrctl collection --delete netflow

solrctl config --delete dpi_dns
solrctl config --delete dpi_http
solrctl config --delete netflow