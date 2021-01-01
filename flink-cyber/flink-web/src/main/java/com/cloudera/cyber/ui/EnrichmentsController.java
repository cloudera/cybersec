package com.cloudera.cyber.ui;

import com.cloudera.cyber.commands.EnrichmentCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/enrichmentLookups")
public class EnrichmentsController {

    // code is not complete  - commenting out for now
  /*  KafkaTemplate<String, EnrichmentCommand> enrichmentTemplate;
    @Value("#{cyber.kafka.enrichment.commandTopic}")
    private String enrichmentTopic;

    public List<EnrichmentLookupSource> getSources() {
        return null;
    }

    @GetMapping("/:name")
    public EnrichmentLookupSource getSource(String name) {
        return null;
    }

    @PutMapping("/:name")
    public void updateSource(String name, EnrichmentLookupSource source) {

    }

    @PostMapping()
    public void addSource(EnrichmentLookupSource source) {

    }

    @PostMapping("/:name/:key")
    public void addEntry(String name, String key, @RequestBody Map<String,String> entries) {
        enrichmentTemplate.send(enrichmentTopic, name + "/" + key, null);
    } */
}
