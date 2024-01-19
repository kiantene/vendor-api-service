package com.nextgen.gameaggregator.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

import java.io.*;

@Service
public class S3Service {

    @Value("${aws.s3.id}")
    private String accessId;
    @Value("${aws.s3.secret}")
    private String accessSecret;
    @Value("${aws.s3.bucket}")
    private String awsBucket;
    @Value("${aws.s3.region}")
    private String awsRegion;
    @Value("${aws.s3.folder}")
    private String awsFolder;
    @Value("${aws.s3.gameUrl}")
    private String gameUrl;

    private AmazonS3 createS3Client() {
        AWSCredentialsProvider doCred = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessId, accessSecret));
        //generate credentials
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(awsRegion))
                .withCredentials(doCred)
                .build();

        return s3Client;
    }

    public String GenerateHtmlToS3(GameSession gameSession, String rawHtml) throws RuntimeException {

        try {
            // Create an S3 client
            AmazonS3 s3Client = createS3Client();

            // Generate filename
            String vendorCode = this.getVendorCode(gameSession.getGameCode());
            String fileName = gameSession.getToken() + ".html";
            String key = vendorCode + "/" + ((isNullOrEmpty(awsFolder)) ? fileName : awsFolder + "/" + fileName);
            uploadHtmlToS3(s3Client, key, rawHtml);

            return gameUrl + key;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void uploadHtmlToS3(AmazonS3 s3Client, String key, String htmlContent) throws IOException {
        // Convert HTML content to input stream
        InputStream inputStream = new ByteArrayInputStream(htmlContent.getBytes());

        // Create metadata for the S3 object
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(htmlContent.getBytes().length);
        metadata.setContentType("text/html");

        // Create a request to upload the object
        PutObjectRequest putObjectRequest = new PutObjectRequest(awsBucket, key, inputStream, metadata);

        // Upload the object to S3
        s3Client.putObject(putObjectRequest);
    }

    private static boolean isNullOrEmpty(String folder) {
        return folder == null || folder.trim().isEmpty();
    }

    private String getVendorCode(String gameCode) {

        String vendorGameCode = gameCode;
        String[] getGameCodeParts = gameCode.split("_");

        // Check if the underscore exists in the string
        if (getGameCodeParts.length > 1) {
            // Extract the substring before the underscore
            vendorGameCode = getGameCodeParts[0];

        }

        return vendorGameCode.toLowerCase();

    }

}
