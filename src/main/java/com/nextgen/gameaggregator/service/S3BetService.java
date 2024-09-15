package com.nextgen.gameaggregator.service;

import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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

    @Value("${aws.s3.bet-bucket.enable:false}")
    private Boolean enableBetBucket;

    @Autowired
    public S3BetService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);

    public void uploadBetHistoryJsonFileAsync(com.nextgen.gameaggregator.entity.warehouse.BetHistory warehouseBetHistory) {

        if (enableBetBucket) {
            THREAD_POOL.submit(() -> {
                uploadBetHistoryJsonFile(warehouseBetHistory);  // Call the actual upload method
            });
        }

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
    public com.nextgen.gameaggregator.entity.warehouse.BetHistory readBetHistoryFromS3File(String betId) {

        String fileName = betId + ".json";
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        try (ResponseInputStream<?> s3Object = s3Client.getObject(getObjectRequest);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s3Object, StandardCharsets.UTF_8))) {

            // Read the file content as a string
            String jsonContent = reader.lines().collect(Collectors.joining());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});

            // Manually map the JSON content to BetHistory object
            com.nextgen.gameaggregator.entity.warehouse.BetHistory betHistory = new com.nextgen.gameaggregator.entity.warehouse.BetHistory();

            betHistory.setId((String) jsonMap.get("id"));
            betHistory.setExternalTransactionId((String) jsonMap.get("externalTransactionId"));
            betHistory.setVendorBetId((String) jsonMap.get("vendorBetId"));
            betHistory.setRoundId((String) jsonMap.get("roundId"));
            betHistory.setVendorGameId((Integer) jsonMap.get("vendorGameId"));
            betHistory.setGameCode((String) jsonMap.get("gameCode"));
            betHistory.setVendorPlayerId(((Number) jsonMap.get("vendorPlayerId")).longValue());
            betHistory.setVendorPlayerUsername((String) jsonMap.get("vendorPlayerUsername"));
            betHistory.setVendorId((Integer) jsonMap.get("vendorId"));
            betHistory.setVendorCode((String) jsonMap.get("vendorCode"));
            betHistory.setVendorLineId((Integer) jsonMap.get("vendorLineId"));
            betHistory.setAgentPlayerId(((Number) jsonMap.get("agentPlayerId")).longValue());
            betHistory.setAgentPlayerUsername((String) jsonMap.get("agentPlayerUsername"));
            betHistory.setAgentId((Integer) jsonMap.get("agentId"));
            betHistory.setOperatorStatus((Integer) jsonMap.get("operatorStatus"));
            betHistory.setGameCategoryId((Integer) jsonMap.get("gameCategoryId"));
            betHistory.setGameCategoryCode((String) jsonMap.get("gameCategoryCode"));
            betHistory.setCurrencyId((Integer) jsonMap.get("currencyId"));
            betHistory.setCurrencyCode((String) jsonMap.get("currencyCode"));
            betHistory.setBetAmount(new BigDecimal(jsonMap.get("betAmount").toString()));
            betHistory.setWinAmount(new BigDecimal(jsonMap.get("winAmount").toString()));
            betHistory.setWinLoss(new BigDecimal(jsonMap.get("winLoss").toString()));
            betHistory.setEffectiveTurnover(new BigDecimal(jsonMap.get("effectiveTurnover").toString()));
            betHistory.setJackpotAmount(new BigDecimal(jsonMap.get("jackpotAmount").toString()));
            betHistory.setResultType((Integer) jsonMap.get("resultType"));
            betHistory.setBetType((Integer) jsonMap.get("betType"));
            betHistory.setIsFreespin((Integer) jsonMap.get("isFreespin"));
            betHistory.setResettleNum((Integer) jsonMap.get("resettleNum"));
            betHistory.setStatus((Integer) jsonMap.get("status"));
            betHistory.setGameSessionToken((String) jsonMap.get("gameSessionToken"));
            betHistory.setVendorBetTime(((Number) jsonMap.get("vendorBetTime")).longValue());
            betHistory.setVendorSettleTime(((Number) jsonMap.get("vendorSettleTime")).longValue());
            betHistory.setResultTime(((Number) jsonMap.get("resultTime")).longValue());

            return betHistory;

        } catch (NoSuchKeyException e) {
            throw new RuntimeException("File not found in S3 with betId :"+betId, e);
        } catch (JsonMappingException | JsonProcessingException e) {
            throw new RuntimeException("Error deserializing JSON from S3 file with betId :"+betId, e);
        } catch (Exception e) {
            throw new RuntimeException("Error reading S3 file with betId :"+betId, e);
        }
    }
}
