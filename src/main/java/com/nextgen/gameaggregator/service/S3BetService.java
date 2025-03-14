package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
public class S3BetService {
    private final S3Client s3Client;

    @Value("${aws.s3.bet-bucket}")
    private String bucketName;

    @Value("${aws.s3.bet-bucket.write:false}")
    private Boolean enableBetBucketWrite;

    @Autowired
    public S3BetService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);

    public void uploadBetHistoryJsonFileAsync(com.nextgen.gameaggregator.entity.warehouse.BetHistory warehouseBetHistory) {

//        if (enableBetBucketWrite) {
//            THREAD_POOL.submit(() -> {
//                uploadBetHistoryJsonFile(warehouseBetHistory);  // Call the actual upload method
//            });
//        }

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
        } catch (Exception e) {
            // Log or handle the exception appropriately
            log.error(e.getMessage() + " -> Error uploading bet history file to S3: =" + warehouseBetHistory.getId());
            e.printStackTrace();
        }
    }

    // Method to read JSON from S3 and convert it to a BetHistory object
    public com.nextgen.gameaggregator.entity.warehouse.BetHistory readBetHistoryFromS3File(String betId) throws Exception {

        String fileName = betId + ".json";
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        try (ResponseInputStream<?> s3Object = s3Client.getObject(getObjectRequest);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s3Object, StandardCharsets.UTF_8))) {

            // Read the file content as a string
            String jsonContent = reader.lines().collect(Collectors.joining());
            jsonContent = convertCamelCaseToSnakeCaseWithUnderscore(jsonContent);

            ObjectMapper objectMapper = new ObjectMapper();

            // Convert the JSON string into BetHistory object using Jackson
            return objectMapper.readValue(jsonContent, com.nextgen.gameaggregator.entity.warehouse.BetHistory.class);


        } catch (NoSuchKeyException e) {
            throw new Exception("File not found in S3 with betId :"+betId);
        } catch (JsonProcessingException e) {
            throw new Exception("Error deserializing JSON from S3 file with betId :"+betId, e);
        } catch (Exception e) {
            throw new RuntimeException("Error reading S3 file with betId :"+betId, e);
        }
    }

    public static String convertCamelCaseToSnakeCaseWithUnderscore(String jsonContent) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Convert JSON string to a Map
        Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});

        // Create a new map to hold the modified keys
        Map<String, Object> convertedMap = new HashMap<>();

        // Iterate over the original map and modify the keys
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            String newKey = convertCamelCaseToSnakeCase(entry.getKey());
            convertedMap.put(newKey, entry.getValue());
        }

        // Convert the updated map back to a JSON string
        return objectMapper.writeValueAsString(convertedMap);
    }

    // Helper function to convert camelCase to snake_case without adding an underscore at the beginning
    private static String convertCamelCaseToSnakeCase(String key) {
        StringBuilder result = new StringBuilder();

        for (char ch : key.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append("_").append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }
}
