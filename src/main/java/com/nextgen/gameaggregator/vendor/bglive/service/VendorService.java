package com.nextgen.gameaggregator.vendor.bglive.service;


import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.bglive.api.gameurl.UserDto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@Slf4j
@Getter
@Setter
public class VendorService extends BaseVendorService {

    private GameSessionService gameSessionService;

    @Autowired
    public VendorService(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    public static String encryptCreateUserMd5Key(String random, String snCode, String secretCode) throws InvalidFormatException {
        try {
            String combined = random + snCode + secretCode;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(combined.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new InvalidFormatException("Encrypt MD5 Fail");
        }
    }

    public static String encryptLoginMd5Key(String random, String snCode, String loginId, String secretCode) throws InvalidFormatException {
        try {
            String combined = random + snCode + loginId + secretCode;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(combined.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new InvalidFormatException("Encrypt MD5 Fail");
        }
    }

    public static String generateSecretCode(String password) throws InvalidFormatException {
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            byte[] sha1Hash = sha1Digest.digest(password.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(sha1Hash);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidFormatException("SHA-1 algorithm not found");
        }
    }

    public static UserDto setUserDto(String random, String digest, String sn, String loginId, String agentLoginId) {
        UserDto userDto = new UserDto();
        userDto.setRandom(random);
        userDto.setDigest(digest);
        userDto.setSn(sn);
        userDto.setLoginId(loginId);
        userDto.setAgentLoginId(agentLoginId);
        return userDto;
    }

//    public static LoginDto setLoginDto(String random, String digest, String sn, String loginId) {
//        LoginDto loginDto = new LoginDto();
//        loginDto.setRandom(random);
//        loginDto.setDigest(digest);
//        loginDto.setSn(sn);
//        loginDto.setLoginId(loginId);
//        return loginDto;
//    }
}