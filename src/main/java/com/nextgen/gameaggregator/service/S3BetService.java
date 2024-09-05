package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class S3BetService {
    private final S3Client s3Client;

    @Value("${aws.s3.bet-bucket}")
    private String bucketName;
    @Autowired
    public S3BetService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);

    public void uploadBetHistoryJsonFileAsync(com.nextgen.gameaggregator.entity.warehouse.BetHistory warehouseBetHistory) {
        THREAD_POOL.submit(() -> {
            uploadBetHistoryJsonFile(warehouseBetHistory);  // Call the actual upload method
        });
    }

    public void uploadBetHistoryJsonFile(com.nextgen.gameaggregator.entity.warehouse.BetHistory warehouseBetHistory) {
        // Customize the file name and use it as the key in S3
        String fileName = warehouseBetHistory.getId() + ".json";

        // Create the PutObjectRequest with the specified bucket name and key (file name)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType("application/json")  // Set content type to JSON
                .build();
        // Serialize BetHistory to JSON using Gson
        Gson gson = new Gson();
        String jsonContent = gson.toJson(warehouseBetHistory);

        try {
            // Use RequestBody.fromString() to upload the JSON content
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(jsonContent.getBytes(StandardCharsets.UTF_8)));
            System.err.println("upload file bet history");
        } catch (Exception e) {
            // Log or handle the exception appropriately
            log.error(e.getMessage() + " -> Error uploading bet history file to S3: =" + warehouseBetHistory.getId() );
            e.printStackTrace();
        }
    }
}
