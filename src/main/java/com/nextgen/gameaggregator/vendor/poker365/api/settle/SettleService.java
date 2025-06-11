package com.nextgen.gameaggregator.vendor.poker365.api.settle;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.poker365.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.poker365.service.VendorService;
import com.nextgen.gameaggregator.vendor.poker365.vo.CommonVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SettleService {
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final VendorPlayerService vendorPlayerService;
    private final OperatorWalletService operatorWalletService;
    private final WalletRequestService walletRequestService;

    public SettleService(HttpService httpService,
                         VendorService vendorService,
                         GameSessionService gameSessionService,
                         RequestIdempotentLogService requestIdempotentLogService,
                         VendorPlayerService vendorPlayerService,
                         OperatorWalletService operatorWalletService,
                         WalletRequestService walletRequestService) {
        this.requestIdempotentLogService = requestIdempotentLogService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorPlayerService = vendorPlayerService;
        this.operatorWalletService = operatorWalletService;
        this.walletRequestService = walletRequestService;
    }

    private void dataMapper(WalletRequest walletRequest, MessageDto dto, GameSession gameSession) {

        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        walletRequest.setExternalTransactionId(dto.getExternalTransactionId());
        walletRequest.setRoundId(dto.getRoundId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(dto.getTxId());
        walletRequest.setTakeAll(0);

        BigDecimal amount = dto.getProfit().subtract(dto.getBonus());
        BigDecimal winAmount = amount.add(dto.getBetAmount());

        walletRequest.setTransferAmount(dto.getPayAmount());
        walletRequest.setBetAmount(dto.getRealBetMoney());

        ResultType resultType = vendorService.calculateResultType(dto.getRealBetMoney(), winAmount, dto.getJackpotAmount(), false);

        walletRequest.setWinAmount(winAmount);
        walletRequest.setEffectiveTurnover(dto.getBetAmount());
        walletRequest.setJackpotAmount(dto.getJackpotAmount());
        walletRequest.setResultType(resultType.code);
        walletRequest.setVendorBetTime(System.currentTimeMillis());
        walletRequest.setVendorSettleTime(System.currentTimeMillis());

    }

    public CommonVo settle(HttpRequestLog httpRequestLog) {
        CommonVo commonVo = new CommonVo();
        BigDecimal balance;
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        Integer vendorPlayerId;

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CommonDto commonDto = VendorService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);
            String formatedMessageDto = commonDto.getMessage();
            MessageDto messageDto = HttpService.convertJsonToDto(formatedMessageDto, MessageDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(commonDto, messageDto);

            vendorPlayerId = Integer.valueOf(messageDto.getUserId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(vendorPlayerId), null);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            if (requestIdempotentLogService.checkExists(messageDto, vendorPlayer.getUsername()) == null) {
                requestIdempotentLogService.create(messageDto, vendorPlayer.getUsername());
            } else {
                throw new TransactionStillProcessingException();
            }

            // 4. Verify remaining parameters (Verify against database values)
            vendorService.doVerification(commonDto, gameSession, messageDto.getUserId(), messageDto.getCurrency(), messageDto.getGameId());

            this.dataMapper(walletRequest, messageDto, gameSession);
            walletRequest = operatorWalletService.betCredit(walletRequest);
            balance = walletRequest.getBalanceAfter();
            // 6. Set response data
            commonVo.setBalance(balance);
            commonVo.setStatus(ResponseCodes.SUCCESS_200.status);

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

        } catch (InvalidPlayerException | NumberFormatException e) {
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


}
