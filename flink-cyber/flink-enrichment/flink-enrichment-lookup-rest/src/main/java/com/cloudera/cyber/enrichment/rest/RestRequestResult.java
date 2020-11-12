package com.cloudera.cyber.enrichment.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class RestRequestResult {
    private Map<String, String> extensions = new HashMap<>();
    private List<String> errors = new ArrayList<>();
}

