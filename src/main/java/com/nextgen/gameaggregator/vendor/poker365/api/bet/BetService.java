package com.nextgen.gameaggregator.vendor.poker365.api.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.poker365.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.poker365.service.VendorService;
import com.nextgen.gameaggregator.vendor.poker365.vo.CommonVo;
import org.springframework.stereotype.Service;

@Service
public class BetService {

    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final VendorPlayerService vendorPlayerService;
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;

    public BetService(HttpService httpService,
                      VendorService vendorService,
                      GameSessionService gameSessionService,
                      VendorPlayerService vendorPlayerService,
                      WalletRequestService walletRequestService,
                      OperatorWalletService operatorWalletService) {
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorPlayerService = vendorPlayerService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
    }


    private void dataMapper(WalletRequest walletRequest, MessageDto dto, GameSession gameSession) {

        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setExternalTransactionId(dto.getExternalTransactionId());
        walletRequest.setRoundId(dto.getRoundId());
        walletRequest.setVendorGameCode(dto.getGameId());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(dto.getTxId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        walletRequest.setTransferAmount(dto.getBetAmount());
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());

    }

    public CommonVo bet(HttpRequestLog httpRequestLog) {
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

            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(messageDto.getUserId()), null);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(String.valueOf(messageDto.getGameId()), gameSession);

            // 4. Verify remaining parameters (Verify against database values)
            vendorService.doVerification(commonDto, gameSession, messageDto.getUserId(), messageDto.getCurrency(), messageDto.getGameId());

            //Map data for walletRequest
            this.dataMapper(walletRequest, messageDto, gameSession);

            //Process full bet data
            walletRequest = operatorWalletService.betDebit(walletRequest);

            commonVo.setBalance(walletRequest.getBalanceAfter());
            commonVo.setResponseCodesandMessage(ResponseCodes.SUCCESS_200);

        } catch (InsufficientBalanceException e) {
            commonVo.setResponseCodesandMessage(ResponseCodes.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, e);

        } catch (GameNotSupportedException e) {
            commonVo.setResponseCodesandMessage(ResponseCodes.INVALID_PARAMETERS);
            httpService.logError(httpRequestLog, e);

        } catch (CurrencyNotSupportedException e) {
            commonVo.setResponseCodesandMessage(ResponseCodes.INVALID_CURRENCY);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidPlayerException | NumberFormatException e) {
            commonVo.setResponseCodesandMessage(ResponseCodes.USERNAME_INVALID);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException e) {
            commonVo.setResponseCodesandMessage(ResponseCodes.NOT_AUTHORIZED);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException e) {
            commonVo.setResponseCodesandMessage(ResponseCodes.INVALID_PARAMETERS);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setResponseCodesandMessage(ResponseCodes.FAIL);
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
