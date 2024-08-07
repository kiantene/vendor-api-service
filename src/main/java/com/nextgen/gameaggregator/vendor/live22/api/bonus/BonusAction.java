package com.nextgen.gameaggregator.vendor.live22.api.bonus;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.live22.constant.Credentials;
import com.nextgen.gameaggregator.vendor.live22.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.live22.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.live22.service.VendorService;
import com.nextgen.gameaggregator.vendor.live22.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BonusAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;

    @Autowired
    public BonusAction(HttpService httpService,
                       GameSessionService gameSessionService,
                       WalletService walletService,
                       VendorService vendorService,
                       VendorLineService vendorLineService) {

        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(path = EndPoints.CASH_BONUS)
    public ResponseVo bonus(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        BonusVo vo = new BonusVo();
        String traceId = httpRequestLog.getId();
        BonusDto dto = new BonusDto();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            dto = HttpService.convertJsonToDto(body, BonusDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());

            this.doVerification(dto, gameSession);

            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, ResultType.BET_WIN, vendorService, httpRequestLog);

            vo.setResponseDateTime(dto.getRequestDateTime());
            vo.setOldBalance((balance.subtract(dto.getWinAmount())).setScale(4, RoundingMode.DOWN));
            vo.setNewBalance(balance.setScale(4, RoundingMode.DOWN));

        } catch (AuthenticationException e) {
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);
            vo.setResponseDateTime(dto.getRequestDateTime()); //set for vendor acceptance test
            vo.setOldBalance(BigDecimal.ZERO);
            vo.setNewBalance(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidSignatureException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_SIGNATURE);
            vo.setResponseDateTime(dto.getRequestDateTime()); //set for vendor acceptance test
            vo.setOldBalance(BigDecimal.ZERO);
            vo.setNewBalance(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidPlayerException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_PLAYER_PASSWORD);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException e) {
            if (e.getMessage() != null && e.getMessage().equals(String.valueOf(ResponseCodes.OPERATOR_ID_ERROR.Status))) {
                vo.setResponseCodes(ResponseCodes.OPERATOR_ID_ERROR); //check db credential (operatorId) with request body value of operatorId that sent from vendor

            } else {
                vo.setResponseCodes(ResponseCodes.INCOMING_REQUEST_INFO_INCOMPLETE);

            }
            httpService.logError(httpRequestLog, e);

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCodes(ResponseCodes.DUPLICATE_TRANSACTION);
            vo.setResponseDateTime(dto.getRequestDateTime()); //set for vendor acceptance test
            vo.setOldBalance(BigDecimal.ZERO);
            vo.setNewBalance(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, e);

        } catch (CurrencyNotSupportedException | CredentialNotFoundException | InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException | TransactionStillProcessingException e) {
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidFormatException e) {
            vo.setResponseCodes(ResponseCodes.INCOMING_REQUEST_INFO_INCOMPLETE);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BonusDto dto, GameSession gameSession)
            throws
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidRequestException,
            InvalidSignatureException,
            CurrencyNotSupportedException {

        // Verify OperatorId
        String operatorId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        ValidationUtils.isEquals(operatorId, dto.getOperatorId(), () -> new InvalidRequestException(String.valueOf(ResponseCodes.OPERATOR_ID_ERROR.Status)));

        // Generate encryptString
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        String encryptString = "CashBonus" + dto.getTranId() + dto.getRequestDateTime() + dto.getOperatorId() + secretKey + dto.getPlayerId();
        String toVerifySign = VendorService.generateSign(encryptString);

        // Verify signature
        VendorService.isSameSignature(dto.getSignature(), toVerifySign);

        // Verify vendor username and currency
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }
}
