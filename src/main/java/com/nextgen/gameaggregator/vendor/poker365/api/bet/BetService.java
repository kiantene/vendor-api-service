package com.nextgen.gameaggregator.vendor.poker365.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.constant.Credentials;
import com.nextgen.gameaggregator.vendor.poker365.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.poker365.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetService {

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
    public BetService(HttpService httpService,
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

    @PostMapping(path = EndPoints.BET)
    public CommonVo bet(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();


        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BetDto betDto = HttpService.convertJsonToDto(body, BetDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(betDto);


            this.vendorPlayerId = Integer.valueOf(betDto.getMessage().getUserId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(vendorPlayerId), null);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());


            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);


            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto,
                    httpRequestLog.getRequestBody(), httpRequestLog);

            // 6. Set response data
            commonVo.setBalance(betEvent.getLastBalance());
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
//        } catch (InvalidRequestException e) {
//            betVo.setError(ErrorVo.from(ResponseCodes.ERR_REGULATORY_GENERAL));
//            httpService.logError(httpRequestLog, e);
//        } catch (InsufficientBalanceException | GameNotSupportedException e) {
//            betVo.setError(ErrorVo.from(ResponseCodes.ERR_INSUFFICIENT_FUNDS));
//            httpService.logError(httpRequestLog, e);
//        } catch (GameNotSupportedException e) {
//            betVo.setError(ErrorVo.from(ResponseCodes.ERR_INSUFFICIENT_FUNDS));
//            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            commonVo.setStatus(ResponseCodes.FAIL.status);
            commonVo.setMsg(ResponseCodes.FAIL.message);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }
        return commonVo;
    }

    private void doValidation(BetDto betDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(betDto);
    }

    private void doVerification(BetDto betDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException, DisabledAgentPlayerException, CredentialNotFoundException, InvalidVendorLineException, InvalidPlayerException, DisabledGameException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String cert = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CERT);
        ValidationUtils.isEquals(cert, betDto.getKey(), InvalidPlayerException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerId()), betDto.getMessage().getUserId(), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}
