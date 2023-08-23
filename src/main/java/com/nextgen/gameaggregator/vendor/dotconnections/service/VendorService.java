package com.nextgen.gameaggregator.vendor.dotconnections.service;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseVo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;

    public static String getSign(String data) {
        String token = DigestUtils.md5Hex(data);
        return token.toUpperCase();
    }

    public static void isSameSignature(String sign, String toVerifySign) throws InvalidSignatureException {
        if (!sign.equals(toVerifySign)) throw new InvalidSignatureException();
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

    /*
    public ResponseVo getCurrentBalanceResponseVo(HttpServletRequest request, String traceId, String brandUid) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        try {
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(brandUid);

            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(walletService.getBalance(traceId, gameSession));
            responseVo.setData(responseDataVo);

        } catch (AuthenticationException authenticationException) {
            responseVo.setCode(ResponseCodes.SIGN_ERROR);
        } catch (InvalidAgentApiCredentialException systemErrorException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (Exception exception) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);
        }

        return responseVo;
    }
     */

    public ResponseVo getCurrentBalanceResponseVo(HttpRequestLog httpRequestLog, String traceId, GameSession gameSession) {

        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        try {
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(walletService.getBalance(traceId, gameSession));
            responseVo.setData(responseDataVo);

        } catch (InvalidAgentApiCredentialException systemErrorException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (Exception exception) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);
        }

        return responseVo;
    }

    @Override
    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        return betInfo.getEffectiveTurnover();
    }
}
