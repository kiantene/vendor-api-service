package com.nextgen.gameaggregator.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

@RestController
@RequestMapping(path = "version/")
@Slf4j
public class VersionController {
    @Autowired
    private Environment environment;

    @Value("${mavenTimestamp}")
    private String timestamp;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Value("${checksum}")
    private String checksum;

    @GetMapping(path = "info")
    public String info() {
        String version = environment.getProperty("project.version");

        String message = String.format(
                "Build Version: %s<br>Checksum: %s<br>Build Time: %s", version, checksum, timestamp);
        return message;
    }
}
