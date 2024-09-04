package com.nextgen.gameaggregator.vendor.cg.api.refund;

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
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import com.nextgen.gameaggregator.vendor.cg.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

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
    public ResponseVo refund(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo refundVo = new ResponseVo();
        try {
            //convert request body into dto
            RefundDto dto = HttpService.convertQueryStringToDtoUrlDecode(httpRequestLog, RefundDto.class);

            //basic validation
            this.doValidation(dto);

            //search for game session using vendor player id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAccountId());
            //basic verification
            this.doVerification(dto, gameSession);

            //rollback process
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);

            //set values
            refundVo.setChannelId(dto.getChannelId());
            refundVo.setAccountId(dto.getAccountId());
            refundVo.setBalance(balance);
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
        } catch (AuthenticationException authenticationException) {
            refundVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_PLAYER);
            httpService.logError(httpRequestLog, authenticationException);
        } catch (Exception e) {
            refundVo.setErrorCode(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, refundVo);
        }

        return refundVo;
    }

    private void doValidation(RefundDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RefundDto dto, GameSession gameSession) throws InvalidVendorLineException, CredentialNotFoundException {
        String channelId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CHANNEL_ID);
        ValidationUtils.isEquals(channelId, dto.getChannelId(), InvalidVendorLineException::new);

    }
}
