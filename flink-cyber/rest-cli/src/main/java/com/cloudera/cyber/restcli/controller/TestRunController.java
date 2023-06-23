package com.cloudera.cyber.restcli.controller;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(value = "/test")
@Slf4j
public class TestRunController {

    @GetMapping(value = "/ls")
    public ResponseEntity<String> runLsCommand() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", "ls");
        Process process = processBuilder.start();
        return ResponseEntity.ok(IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8));
    }
}
