package com.nextgen.gameaggregator.vendor.poker365.api.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
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

@Service
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
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;
    Integer vendorPlayerId;

    @Autowired
    public BetService(HttpService httpService,
                      ValidationService validationService,
                      WalletService walletService,
                      VendorService vendorService,
                      GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      AgentPlayerService agentPlayerService,
                      VendorPlayerService vendorPlayerService,
                      WalletRequestService walletRequestService,
                      OperatorWalletService operatorWalletService) {
        this.validationService = validationService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
    }


    private void dataMapper(WalletRequest walletRequest, MessageDto dto, GameSession gameSession) {

        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setExternalTransactionId(dto.getRoundId());
        walletRequest.setRoundId(dto.getRoundId());
        walletRequest.setVendorGameCode(dto.getGameId());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(dto.getTxId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        walletRequest.setTransferAmount(dto.getBetAmount());
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());

    }

    public CommonVo bet(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
        WalletRequest walletRequest = null;

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CommonDto commonDto = VendorService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);
            String formatedMessageDto = commonDto.getMessage();
            MessageDto messageDto = HttpService.convertJsonToDto(formatedMessageDto, MessageDto.class);
            walletRequest = WalletRequestService.init(httpRequestLog);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(commonDto, messageDto);

            this.vendorPlayerId = Integer.valueOf(messageDto.getUserId());

            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(vendorPlayerId), null);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(String.valueOf(messageDto.getGameId()), gameSession);

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, messageDto, gameSession);

            //Map data for walletRequest
            this.dataMapper(walletRequest, messageDto, gameSession);

            //Process full bet data
            walletRequest = operatorWalletService.betDebit(walletRequest);

        if ("Cancel_Poker365".equals(String.valueOf(gameSession.getVendorPlayerUsername()))) {
            // 6. Set response data
            commonVo.setBalance(null);
            commonVo.setStatus(null);

        }else {
            commonVo.setBalance(walletRequest.getBalanceAfter());
            commonVo.setStatus(ResponseCodes.SUCCESS_200.status);

        }

        } catch (InsufficientBalanceException e) {
            commonVo.setStatus(ResponseCodes.INSUFFICIENT_BALANCE.status);
            commonVo.setMsg(ResponseCodes.INSUFFICIENT_BALANCE.message);
            httpService.logError(httpRequestLog, e);

        } catch (GameNotSupportedException e) {
            commonVo.setStatus(ResponseCodes.GAME_ID_NOT_EXIST.status);
            commonVo.setMsg(ResponseCodes.GAME_ID_NOT_EXIST.message);
            httpService.logError(httpRequestLog, e);

        } catch (CurrencyNotSupportedException e) {
            commonVo.setStatus(ResponseCodes.INVALID_CURRENCY.status);
            commonVo.setMsg(ResponseCodes.INVALID_CURRENCY.message);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidPlayerException e) {
            commonVo.setStatus(ResponseCodes.USERNAME_INVALID.status);
            commonVo.setMsg(ResponseCodes.USERNAME_INVALID.message);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException e) {
            commonVo.setStatus(ResponseCodes.NOT_AUTHORIZED.status);
            commonVo.setMsg(ResponseCodes.NOT_AUTHORIZED.message);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException e) {
            commonVo.setStatus(ResponseCodes.INVALID_PARAMETERS.status);
            commonVo.setMsg(ResponseCodes.INVALID_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setStatus(ResponseCodes.FAIL.status);
            commonVo.setMsg(ResponseCodes.FAIL.message);
            httpService.logError(httpRequestLog, e);

        } finally {
            walletRequestService.end(walletRequest, httpRequestLog, commonVo);

        }
        return commonVo;
    }

    private void doValidation(CommonDto commonDto, MessageDto messageDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonDto);
        ValidationUtils.validateRequest(messageDto);
    }

    private void doVerification(CommonDto commonDto, MessageDto messageDto, GameSession gameSession)
            throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            CredentialNotFoundException,
            InvalidVendorLineException,
            InvalidPlayerException,
            DisabledGameException,
            CurrencyNotSupportedException,
            GameNotSupportedException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());

        Integer vendorLineId = vendorLine.getId();

        String cert = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CERT);

        ValidationUtils.isEquals(cert, commonDto.getKey(), AuthenticationException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerId()), messageDto.getUserId(), InvalidPlayerException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), messageDto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), messageDto.getGameId(), GameNotSupportedException::new);
    }
}
