package com.nextgen.gameaggregator.vendor.bgaming.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.repository.RawSettledBetRepository;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.vendor.bgaming.api.gameurl.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    private static final String HASH_ALGORITHM = "HmacSHA256";
    @Autowired
    RawSettledBetRepository rawSettledBetRepository;
    @Autowired
    private SettledBetService settledBetService;

    public static Long getTimestamp() {
        return Instant.now().toEpochMilli();
    }

    public static void verifySign(String authToken, String data, String sign) throws InvalidSignatureException {

        try {
            // Create a new secret key based on the given API secret
            SecretKeySpec keySpec = new SecretKeySpec(authToken.getBytes(), HASH_ALGORITHM);

            // Generate a message authentication code (MAC) from the input and key
            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes());

            // Convert the MAC to a hexadecimal string format
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            if (!sign.equals(sb.toString())) {
                String msg = "Expected sign: " + sb + ", but received: " + sign;
                log.error("Request body: " + data);
                log.error(msg);
                throw new InvalidSignatureException();
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }

    public UserDto setUserDto(GameSession gameSession) {
        UserDto userDto = new UserDto();
        userDto.setId(gameSession.getVendorPlayerUsername());
        return userDto;
    }

    public String generateSign(String authToken, String data) {

        try {
            // Create a new secret key based on the given API secret
            SecretKeySpec keySpec = new SecretKeySpec(authToken.getBytes(), HASH_ALGORITHM);

            // Generate a message authentication code (MAC) from the input and key
            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes());

            // Convert the MAC to a hexadecimal string format
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        //Temporary only BGAMING, SpadeGaming, EvoNetent need to accept cancel request
        return false;
    }

    public void verifySettledRound(Long vendorPlayerId, String roundId) throws BetResultIdempotentViolationException {
        List<SettledBet> settledBetList = rawSettledBetRepository.findByVendorPlayerIdAndRoundId(vendorPlayerId, roundId);
        if (settledBetList != null && !settledBetList.isEmpty()) {
            throw new BetResultIdempotentViolationException();
        }
    }
}
