package com.nextgen.gameaggregator.vendor.advantplay.api.refund;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.advantplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.advantplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.advantplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.advantplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.advantplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.advantplay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RefundBetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.REFUND_BET)
    public ResponseVo refundBetAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo vo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            String apHash = request.getHeader(Formats.AP_HASH);
            RefundBetDto refundBetDto = HttpService.convertJsonToDto(body, RefundBetDto.class);

            vo.setSeq(refundBetDto.getSeq());

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(refundBetDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(refundBetDto.getPlayerId(), refundBetDto.getGameCode());

            // 3. get Bet History for checking
            this.doVerification(refundBetDto, gameSession, apHash, body);

            // 4. Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, refundBetDto, gameSession, vendorService, httpRequestLog);

            vo.setTimestamp(VendorService.getTimestamp());
            vo.setBalance(balance);

        } catch (AuthenticationException e) {
            vo.setResponseCodes(ResponseCodes.TOKEN_INVALID);
            httpService.logError(httpRequestLog, e);

        } catch (GameNotSupportedException e) {
            vo.setResponseCodes(ResponseCodes.GAME_NOT_FOUND);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException |
                 JsonProcessingException |
                 VendorCurrencyNotSupportException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledGameException e) {
            vo.setResponseCodes(ResponseCodes.PARAMETER_INCORRECT);
            httpService.logError(httpRequestLog, e);

        } catch (DisabledAgentPlayerException e) {
            vo.setResponseCodes(ResponseCodes.ACCOUNT_LOCKED);
            httpService.logError(httpRequestLog, e);

        } catch (RecordNotFoundException |
                 BetNotFoundException e) {
            vo.setResponseCodes(ResponseCodes.DATA_INVALID);
            httpService.logError(httpRequestLog, e);

        } catch (BetRefundIdempotentViolationException | BetResultIdempotentViolationException e) {
            vo.setResponseCodes(ResponseCodes.DUPLICATE_REQUEST);

        } catch (InvalidOperatorResponseException | TransactionStillProcessingException e) {
            vo.setResponseCodes(ResponseCodes.UNSPECIFIED_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.UNSPECIFIED_ERROR);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RefundBetDto dto, GameSession gameSession, String apHash, String bodyString)
            throws
            GameNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidRequestException {

        // Verify vendor request ap-hash
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
        ValidationUtils.isEquals(vendorService.generateHash(secretKey, bodyString), apHash);

        // Verify vendor gameCode, username
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGameCode()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);

    }
}
