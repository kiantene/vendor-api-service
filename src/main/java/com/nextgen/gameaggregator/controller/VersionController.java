package com.nextgen.gameaggregator.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "version/")
@Slf4j
public class VersionController {

    @GetMapping(path = "info")
    public String info() {
        String version = "1.0.13.1";
        String checksum = "2c9bb26333c460d75062f29929abf6578237de5a6019a3d0c849965dd6867c68";
        String versionLabel = "Build Version: " + version;
        String checksumLabel = "Checksum: " + checksum;

        return versionLabel + "<br>" + checksumLabel;
    }
}
