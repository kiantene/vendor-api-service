package com.nextgen.gameaggregator.vendor.poker365.api.settle;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
    private final OperatorWalletService operatorWalletService;
    private final WalletRequestService walletRequestService;
    Integer vendorPlayerId;

    @Autowired
    public SettleService(HttpService httpService,
                         ValidationService validationService,
                         WalletService walletService,
                         VendorService vendorService,
                         GameSessionService gameSessionService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService, VendorPlayerService vendorPlayerService, OperatorWalletService operatorWalletService, WalletRequestService walletRequestService) {
        this.validationService = validationService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
        this.operatorWalletService = operatorWalletService;
        this.walletRequestService = walletRequestService;
    }

    private void dataMapper(WalletRequest walletRequest, MessageDto dto, GameSession gameSession) {

        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        walletRequest.setExternalTransactionId(dto.getRoundId());
        walletRequest.setRoundId(dto.getRoundId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(dto.getTxId());
        //walletRequest.setAction("credit");
        walletRequest.setTakeAll(0);
        walletRequest.setTransferAmount(dto.getProfit());
        walletRequest.setBetAmount(dto.getRealBetMoney());
//        BigDecimal winAmount = dto.getBonus().compareTo(BigDecimal.ZERO) > 0
//                ? dto.getProfit().subtract(dto.getBonus())
//                : dto.getProfit();

        BigDecimal winAmount;
        if (dto.getProfit().compareTo(BigDecimal.ZERO) > 0) {
            winAmount = (dto.getBonus().compareTo(BigDecimal.ZERO) > 0)
                    ? dto.getProfit().subtract(dto.getBonus())
                    : dto.getProfit();
        } else {
            winAmount = BigDecimal.ZERO;
        }

        walletRequest.setWinAmount(winAmount);
        walletRequest.setEffectiveTurnover(BigDecimal.ZERO);
        walletRequest.setJackpotAmount(dto.getJackpotAmount());
        walletRequest.setResultType(ResultType.BET_WIN.code);
        walletRequest.setVendorBetTime(System.currentTimeMillis());
        walletRequest.setVendorSettleTime(System.currentTimeMillis());

        walletRequest.setWinLoss(dto.getProfit().add(dto.getBetAmount()));
    }

    public CommonVo settle(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
        BigDecimal balance;
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
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

//            ResultType resultType = vendorService.calculateResultType(BigDecimal.ZERO, messageDto.getWinAmount(), messageDto.getJackpotAmount(), false);

//            balance = walletService.processBetResult(traceId, gameSession, messageDto, resultType, vendorService, httpRequestLog);

            this.dataMapper(walletRequest, messageDto, gameSession);
            walletRequest = operatorWalletService.betCredit(walletRequest);
            balance = walletRequest.getBalanceAfter();
            // 6. Set response data
            commonVo.setBalance(balance);
            commonVo.setStatus(ResponseCodes.SUCCESS_200.status);


//        } catch (BetResultIdempotentViolationException | TransactionStillProcessingException e) {
//            commonVo.setStatus(ResponseCodes.NO_DATA.status);
//            commonVo.setMsg(ResponseCodes.NO_DATA.message);
//            httpService.logError(httpRequestLog, e);

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
            DisabledVendorLineException, DisabledAgentPlayerException, CredentialNotFoundException, InvalidVendorLineException, InvalidPlayerException, DisabledGameException, CurrencyNotSupportedException, GameNotSupportedException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String cert = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CERT);
        ValidationUtils.isEquals(cert, commonDto.getKey(), AuthenticationException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerId()), String.valueOf(messageDto.getUserId()), InvalidPlayerException::new);
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
