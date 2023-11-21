package com.nextgen.gameaggregator.vendor.yeebet.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.security.MessageDigest;

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
}
