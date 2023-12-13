package com.nextgen.gameaggregator.vendor.advantplay.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.advantplay.constant.EndPoints;
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
public class SettleAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = EndPoints.SETTLE)
    public ResponseVo settleAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo vo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
//            String apHash = request.getHeader("ap-hash");
            SettleDto settleDto = HttpService.convertJsonToDto(body, SettleDto.class);

            vo.setSeq(settleDto.getSeq());

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(settleDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(settleDto.getPlayerId(), settleDto.getGameId());

            this.doVerification(settleDto, gameSession);

            ResultType resultType = vendorService.calculateResultType(settleDto.getBetStatus(), settleDto.getWinAmount(), settleDto.getJackpotAmount(), false);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleDto, resultType, vendorService, httpRequestLog);

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

        } catch (BetNotFoundException e) {
            vo.setResponseCodes(ResponseCodes.DATA_INVALID);
            httpService.logError(httpRequestLog, e);

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCodes(ResponseCodes.DUPLICATE_REQUEST);
            httpService.logError(httpRequestLog, e);

        } catch (InsufficientBalanceException e) {
            vo.setResponseCodes(ResponseCodes.PLAYER_HAS_INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);

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

    private void doVerification(SettleDto dto, GameSession gameSession)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidRequestException {

        // Verify vendor request ap-hash
//        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
//        ValidationUtils.isEquals(vendorService.generateHash(secretKey, bodyString), apHash);

        // Verify vendor gameCode and username
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);

    }
}
