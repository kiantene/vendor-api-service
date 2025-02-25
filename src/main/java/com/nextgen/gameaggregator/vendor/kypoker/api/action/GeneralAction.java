package com.nextgen.gameaggregator.vendor.kypoker.api.action;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.api.balance.BalanceDto;
import com.nextgen.gameaggregator.vendor.kypoker.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.kypoker.constant.Actions;
import com.nextgen.gameaggregator.vendor.kypoker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private final HttpService httpService;
    private final BalanceService balanceService;
    private final BetService betService;
    private final SettleService settleService;
    private final RefundService refundService;
    private final RollbackService rollbackService;
    private final VendorLineService vendorLineService;
    private final ValidationService validationService;

    public GeneralAction(HttpService httpService, BalanceService balanceService, BetService betService, SettleService settleService, RefundService refundService, RollbackService rollbackService, VendorLineService vendorLineService, ValidationService validationService) {
        this.httpService = httpService;
        this.balanceService = balanceService;
        this.betService = betService;
        this.settleService = settleService;
        this.refundService = refundService;
        this.rollbackService = rollbackService;
        this.vendorLineService = vendorLineService;
        this.validationService = validationService;
    }

    @PostMapping
    public CommonVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo vo = new CommonVo();


        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            CommonDto commonDto = HttpService.convertQueryStringToDto(body, CommonDto.class);

            // Validate request parameters (Non-database related)
            ValidationUtils.validateRequest(commonDto);

            // Get the first vendor line id from list
            Integer vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.MERCHANT_ID,request.getHeader("X-Merchant-Id"));

            String secretKey = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.MERCHANT_KEY);
            checkSign(body, request, secretKey);

            // Handle the action and return the resulting value
            vo = this.actionHandling(body, traceId, httpRequestLog, commonDto);

        } catch (Exception e) {
            vo.setErrorCode(ResponseCodes.INTERNAL_ERROR);
            vo.setErrorDescription(ResponseCodes.INTERNAL_ERROR);
            vo.setBalance(null);
        }
        finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws InvalidPlayerException,
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, AuthenticationException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getPlayerId());

    }

    private CommonVo actionHandling(String actionDto, String traceId, HttpRequestLog httpRequestLog, CommonDto commonDto) {
        CommonVo vo = new CommonVo();
        switch (commonDto.getAction()) {
            case Actions.GET_BALANCE -> vo = balanceService.balance(actionDto, traceId, httpRequestLog);
            case Actions.BET -> vo = betService.bet(actionDto, traceId, httpRequestLog);
            case Actions.SETTLE -> vo = settleService.settle(actionDto, traceId, httpRequestLog);
            case Actions.REFUND -> vo = refundService.refund(actionDto, traceId, httpRequestLog);
            case Actions.ROLLBACK -> vo = rollbackService.rollback(actionDto, traceId, httpRequestLog);

            default -> vo.setErrorCode(ResponseCodes.INTERNAL_ERROR);
        }

        return vo;
    }

    private void checkSign(String body, HttpServletRequest request, String secretKey)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, InvalidEncryptionException {

        // Parse the body into a MultiValueMap
        MultiValueMap<String, String> bodyMultiMap = new LinkedMultiValueMap<>();
        if (body != null && !body.trim().isEmpty()) {
            String[] params = body.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = URLDecoder.decode(keyValue[1], "UTF-8");
                    bodyMultiMap.add(key, value);
                }
            }
        }

        // Initialize the header MultiValueMap
        MultiValueMap<String, String> headerMultiMap = new LinkedMultiValueMap<>();

        // Add headers to the header MultiValueMap
        headerMultiMap.add("X-Merchant-Id", request.getHeader("X-Merchant-Id"));
        headerMultiMap.add("X-Timestamp", request.getHeader("X-Timestamp"));
        headerMultiMap.add("X-Nonce", request.getHeader("X-Nonce"));

        // Generate the encrypted key
        String encryptedKey = VendorService.HmacSha1Sign(bodyMultiMap, headerMultiMap, secretKey);

        // Validate the encrypted key
        try {
            ValidationUtils.isEquals(encryptedKey, request.getHeader("X-Sign"));
        } catch (InvalidRequestException e) {
            throw new InvalidEncryptionException();
        }
    }

}