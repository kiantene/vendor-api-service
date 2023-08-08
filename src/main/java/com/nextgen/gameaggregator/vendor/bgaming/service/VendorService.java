package com.nextgen.gameaggregator.vendor.bgaming.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.bgaming.api.gameurl.UserDto;
import com.nextgen.gameaggregator.vendor.bgaming.constant.CurrencyDecimals;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    private static final String HASH_ALGORITHM = "HmacSHA256";

    public static Long getTimestamp() {
        return Instant.now().toEpochMilli();
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

    public Integer convertAmountToInteger(BigDecimal amount, String currencyCode) throws CurrencyNotSupportedException {
        // TODO: If have new currency code need to update below method for convert Integer
        Integer decimal = CurrencyDecimals.CURRENCY_DECIMAL.get(currencyCode);
        if (decimal == null) {
            throw new CurrencyNotSupportedException();
        }
        return amount.multiply(new BigDecimal(decimal)).intValue();
    }

    public BigDecimal convertAmountToBigDecimal(Integer amount, String currencyCode) throws CurrencyNotSupportedException {
        // TODO: If have new currency code need to update below method for convert Integer
        Integer decimal = CurrencyDecimals.CURRENCY_DECIMAL.get(currencyCode);
        if (decimal == null) {
            throw new CurrencyNotSupportedException();
        }
        return new BigDecimal(amount).divide(new BigDecimal(decimal));
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        //Temporary only BGAMING, SpadeGaming, EvoNetent need to accept cancel request
        return false;
    }
}
