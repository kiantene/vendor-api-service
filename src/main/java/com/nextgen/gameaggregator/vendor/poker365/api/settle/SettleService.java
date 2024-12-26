package com.nextgen.gameaggregator.vendor.poker365.api.settle;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.constant.Credentials;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.poker365.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.poker365.service.VendorService;
import com.nextgen.gameaggregator.vendor.poker365.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class SettleService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorPlayerService vendorPlayerService;
    Integer vendorPlayerId;

    @Autowired
    public SettleService(HttpService httpService,
                         ValidationService validationService,
                         WalletService walletService,
                         VendorService vendorService,
                         GameSessionService gameSessionService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService, VendorPlayerService vendorPlayerService) {
        this.validationService = validationService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
    }

    public CommonVo settle(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
        BigDecimal balance;

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CommonDto commonDto = VendorService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);
            String formatedMessageDto = commonDto.getMessage();
            MessageDto messageDto = HttpService.convertJsonToDto(formatedMessageDto, MessageDto.class);

//            List<TransactionsDto> transactionsDto = messageDto.getTransactionsDto();
            // 2. Validate request parameters (Non-database calls)
            this.doValidation(commonDto, messageDto);


            this.vendorPlayerId = Integer.valueOf(messageDto.getUserId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(vendorPlayerId), null);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());


            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, messageDto, gameSession);

            ResultType resultType = vendorService.calculateResultType(BigDecimal.ZERO, messageDto.getWinAmount(), messageDto.getJackpotAmount(), false);

            balance = walletService.processBetResult(traceId, gameSession, messageDto, resultType, vendorService, httpRequestLog);


            // 6. Set response data
            commonVo.setBalance(balance);
            commonVo.setStatus(ResponseCodes.SUCCESS_200.status);


//        } catch (InvalidPlayerException e) {
//            betVo.setError(ErrorVo.from(ResponseCodes.ERR_PLAYER_NOT_FOUND));
//            httpService.logError(httpRequestLog, e);
//        } catch (BetResultIdempotentViolationException | TransactionStillProcessingException e) {
//            betVo.setError(ErrorVo.from(ResponseCodes.ERR_TRANSACTION_DECLINED));
//            httpService.logError(httpRequestLog, e);
//        } catch (AuthenticationException e) {
//            betVo.setError(ErrorVo.from(ResponseCodes.ERR_AUTHENTICATION_FAILED));
//            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            commonVo.setStatus(ResponseCodes.FAIL.status);
            commonVo.setMsg(ResponseCodes.FAIL.message);
            httpService.logError(httpRequestLog, e);
//        } catch (InsufficientBalanceException | GameNotSupportedException e) {
//            betVo.setError(ErrorVo.from(ResponseCodes.ERR_INSUFFICIENT_FUNDS));
//            httpService.logError(httpRequestLog, e);
//        } catch (GameNotSupportedException e) {
//            betVo.setError(ErrorVo.from(ResponseCodes.ERR_INSUFFICIENT_FUNDS));
//            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            commonVo.setStatus(ResponseCodes.USERNAME_INVALID.status);
            commonVo.setMsg(ResponseCodes.USERNAME_INVALID.message);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }
        return commonVo;
    }

    private void doValidation(CommonDto commonDto, MessageDto messageDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonDto);
        ValidationUtils.validateRequest(messageDto);

    }

    private void doVerification(CommonDto commonDto, MessageDto messageDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException, DisabledAgentPlayerException, CredentialNotFoundException, InvalidVendorLineException, InvalidPlayerException, DisabledGameException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String cert = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CERT);
        ValidationUtils.isEquals(cert, commonDto.getKey(), InvalidPlayerException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerId()), String.valueOf(messageDto.getUserId()), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}
