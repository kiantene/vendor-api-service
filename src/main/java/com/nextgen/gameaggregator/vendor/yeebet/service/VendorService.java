package com.nextgen.gameaggregator.vendor.yeebet.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    public static String generateSign(MultiValueMap<String, String> formData, String secret_key){

        // for loop to concate key and value as string format
        StringBuilder combinedValues = new StringBuilder();

        for(String key : formData.keySet()) {
            for (String value : formData.get(key)) {

                //check combinedValues length is more than 1 or not(equal to 0 means it is first value, so no need to add "&")
                if(combinedValues.length() > 1){
                    combinedValues.append("&");
                }

                combinedValues.append(key).append("=").append(value);
            }
        }

        //add secret key at the end of the combinedValues before MD5 hash
        combinedValues.append("&key=").append(secret_key);

        return md5Generator(combinedValues.toString());
    }

    public static String md5Generator(String input) {

        String md5Hash = null;

        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = input.getBytes("UTF-8");
            md.update(bytes);

            byte[] digest = md.digest();

            StringBuilder hexString = new StringBuilder(input.length() * 2);

            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }

            md5Hash = hexString.toString();
        }catch(Exception e){
            md5Hash = null;
        }

        return md5Hash;
    }

    public static long getTimeStamp(long ori_time){ return ori_time * 1000;}

    public static MultiValueMap<String, String> convertToSortedMultiValueMap(String input) {

        // Split the input string by "&" to get individual key-value pairs
        String[] array = input.split("&");

        // Convert into List
        List<String> sortedKeys = new ArrayList<>();
        for (String element : array) {
            sortedKeys.add(element);
        }

        // Sort array key according to ACSII order
        Collections.sort(sortedKeys);

        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();

        for (String element : sortedKeys) {
            // Split each element by "=" to get key and value
            String[] keyValue = element.split("=");

            // Ensure that the pair has both key and value
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];

                // Add the key-value pair to the MultiValueMap
                multiValueMap.add(key, value);
            }
        }

        return multiValueMap;
    }

    // urldecode request data
    public static String urlDecode(String queryString){
        String converted_body = null;

        try{
            converted_body = URLDecoder.decode(queryString, StandardCharsets.UTF_8.name());

        } catch (Exception exception) {

        }

        return converted_body;
    }

    public static String trimGameCode(String gameCode){

        String trimmedGameCode = null;

        // check if game code contain _stg (ignore case-sensitive)
        if(gameCode.toLowerCase().contains("_stg")){
            // Trim value by removing _stg (ignore case-sensitive)
            trimmedGameCode = gameCode.replaceFirst("(?i)_stg$", "");
        }else{
            // let trimmedCode same as gameCode
            trimmedGameCode = gameCode;
        }

        return trimmedGameCode;
    }
}
