package com.nextgen.gameaggregator.vendor.cg.api.refund;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cg.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import com.nextgen.gameaggregator.vendor.cg.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(EndPoints.PATH)
public class RefundAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;

    @Autowired
    public RefundAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, VendorService vendorService, VendorLineService vendorLineService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(EndPoints.REFUND)
    public String refund(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo refundVo = new ResponseVo();
        CommonDto dto = new CommonDto();
        try {
            //convert body into dto
            dto = HttpService.convertQueryStringToDto(httpRequestLog, CommonDto.class);
            dto.setData(VendorService.urlDecode(dto.getData()));

            //basic validation
            this.doValidation(dto);

            String decryptedData = vendorService.decryptData(dto.getData(), dto.getChannelId());//we get the json here
            httpRequestLog.setRequestBody(decryptedData);
            RefundDto refundDto = HttpService.convertJsonToDto(decryptedData, RefundDto.class);

            GameSession gameSession;
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(refundDto.getAccountId());
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(refundDto.getAccountId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }
            //basic verification
            this.doVerification(refundDto, gameSession);

            //rollback process
            BigDecimal balance = walletService.processRollback(traceId, refundDto, gameSession, vendorService, httpRequestLog);

            //set values
            refundVo.setChannelId(refundDto.getChannelId());
            refundVo.setAccountId(refundDto.getAccountId());
            refundVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            refundVo.setCurrency(gameSession.getVendorCurrencyCode());
            refundVo.setErrorCode(ResponseCodes.SUCCESS);
            refundVo.setReturnTime(VendorService.returnTime());
        } catch (BetRefundIdempotentViolationException |
                 BetResultIdempotentViolationException betRefundIdempotentViolationException) {
            refundVo.setErrorCode(ResponseCodes.SEAMLESS_MTCODE_REFUNDED);
            httpService.logError(httpRequestLog, betRefundIdempotentViolationException);
        } catch (InvalidVendorLineException invalidVendorLineException) {
            refundVo.setErrorCode(ResponseCodes.CHANNEL_ID_ERROR);
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (InvalidRequestException invalidRequestException) {
            refundVo.setErrorCode(ResponseCodes.SEAMLESS_INPUT_ERROR);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (BetNotFoundException betNotFoundException) {
            refundVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_TRANSACTION);
            httpService.logError(httpRequestLog, betNotFoundException);
        } catch (Exception e) {
            refundVo.setErrorCode(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            try {
                String jsonString = new Gson().toJson(refundVo);
                refundVo.setEncrypt(vendorService.encryptResponse(jsonString, dto.getChannelId())); //encrypt the whole vo include error
                httpService.end(httpRequestLog, refundVo);
            } catch (CredentialNotFoundException e) {
                httpService.logError(httpRequestLog, e);
            }
        }

        return refundVo.getEncrypt();
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RefundDto dto, GameSession gameSession) throws InvalidVendorLineException, CredentialNotFoundException {
        String channelId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CHANNEL_ID);
        ValidationUtils.isEquals(channelId, dto.getChannelId(), InvalidVendorLineException::new);

    }
}
