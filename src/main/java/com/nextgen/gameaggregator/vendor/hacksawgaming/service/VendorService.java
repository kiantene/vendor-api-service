package com.nextgen.gameaggregator.vendor.hacksawgaming.service;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {

    public static String getSign(String data) {
        String token = DigestUtils.md5Hex(data);
        return token.toUpperCase();
    }

    public static boolean isSameSignature(String sign, String toVerifySign) {
        Boolean result = false;
        if(sign.equals(toVerifySign)) {
            result = true;
        }
        return result;
    }

    public static String removeDashes(String str) {
        return str.replaceAll("-", "");
    }

    public static String revertToUUID(String uuidString) {
        StringBuilder sb = new StringBuilder(uuidString);
        sb.insert(8, "-");
        sb.insert(13, "-");
        sb.insert(18, "-");
        sb.insert(23, "-");

        return sb.toString();
    }

    @Override
    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        return betInfo.getEffectiveTurnover();
    }
}
