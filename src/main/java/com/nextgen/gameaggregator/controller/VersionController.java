package com.nextgen.gameaggregator.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping(path = "version/")

public class VersionController {
    @Value("${mavenTimestamp}")
    private String timestamp;

    @Value("${version}")
    private String version;

    @Value("${checksumPath}")
    private String checksumPath;

    public static String calculateChecksum(String filePath, String algorithm) throws Exception {
        Path path = Paths.get(filePath);
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        try (DigestInputStream dis = new DigestInputStream(new FileInputStream(path.toFile()), digest)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = dis.read(buffer)) != -1) {
                // Read the file to update the digest
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();

        for (byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    @GetMapping(path = "info")
    public String info() {
        String checksum = "-";
        String algorithm = "SHA-256";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, formatter);
        zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Singapore"));
        String timezoneTimestamp = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd K:mm:ssa z"));
        try {
            checksum = calculateChecksum(checksumPath, algorithm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String message = String.format(
                "Build Version: %s<br>Build Time: %s", version, timezoneTimestamp);
        return message;
    }
}
