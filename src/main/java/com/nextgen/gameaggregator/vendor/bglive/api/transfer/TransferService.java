package com.nextgen.gameaggregator.vendor.bglive.api.transfer;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransferService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final com.nextgen.gameaggregator.vendor.bglive.service.VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;

    @Autowired
    public TransferService(HttpService httpService,
                           WalletService walletService,
                           GameSessionService gameSessionService,
                           VendorLineService vendorLineService,
                           AgentPlayerService agentPlayerService,
                           VendorService vendorService, WalletRequestService walletRequestService, OperatorWalletService operatorWalletService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
    }

    public CommonVo transfer(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
        try {

            String body = httpRequestLog.getRequestBody();
            TransferDto transferDto = HttpService.convertJsonToDto(body, TransferDto.class);
            WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
            // Handle the action and return the resulting value
            this.doValidation(transferDto);

            GameSession gameSession = getGameSession(transferDto);
            this.doVerification(transferDto, gameSession);

            walletRequest = processTransferInOut(transferDto, walletRequest, gameSession, traceId, httpRequestLog);

            commonVo.setSuccessResponse(transferDto.getId(), walletRequest.getBalanceAfter());
//
//        } catch (InsufficientBalanceException e) {
//            //set Vo
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.INSUFFICIENT_BALANCE.code,
//                    ResponseCodes.INSUFFICIENT_BALANCE.message, ResponseCodes.INSUFFICIENT_BALANCE.message);
//            httpService.logError(httpRequestLog, e);
//
//        } catch (InvalidRequestException e) {
//            //set Vo
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
//                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
//            httpService.logError(httpRequestLog, e);
//
//        } catch (InvalidPlayerException e) {
//
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.PLAYER_INVALID.code,
//                    ResponseCodes.PLAYER_INVALID.message, ResponseCodes.PLAYER_INVALID.message);
//            httpService.logError(httpRequestLog, e);
//
//        } catch (AuthenticationException e) {
//
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.AUTH_INVALID.code,
//                    ResponseCodes.AUTH_INVALID.message, ResponseCodes.AUTH_INVALID.message);
//            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);

        }
        return commonVo;
    }

    private GameSession getGameSession(TransferDto transferDto) throws AuthenticationException {
        return gameSessionService.getGameSessionByVendorPlayerUsername(transferDto.getParamsDto().getLoginId());
    }

    private void doValidation(TransferDto transferDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(transferDto);

        ParamsDto paramsDto = transferDto.getParamsDto();
        if (paramsDto != null) {
            ValidationUtils.validateRequest(paramsDto);
        }
    }

    private void doVerification(TransferDto transferDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidFormatException {

        String snCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SN_CODE);
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(snCode, transferDto.getParamsDto().getSn(), InvalidPlayerException::new);

        String validateSign = VendorService.encryptBetMd5Key(transferDto.getParamsDto().getRandom(), snCode,
                gameSession.getVendorPlayerUsername(), String.valueOf(transferDto.getParamsDto().getAmount()), secretKey);
        ValidationUtils.isEquals(validateSign, transferDto.getParamsDto().getSign(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }

    private WalletRequest processTransferInOut(TransferDto transferDto, WalletRequest walletRequest, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog) throws
            VendorCurrencyNotSupportException,
            InsufficientBalanceException,
            InvalidOperatorResponseException,
            InternalServerException,
            InvalidRequestException,
            BetNotAllowedException {

        if (transferDto.getParamsDto().getAmount().compareTo(BigDecimal.ZERO) < 0) {
            this.dataDebitMapper(walletRequest, transferDto, gameSession);
            walletRequest = operatorWalletService.betDebit(walletRequest);
        } else {
            this.dataCreditMapper(walletRequest, transferDto, gameSession);
            walletRequest = operatorWalletService.betCredit(walletRequest);
        }
        return walletRequest;
    }

    private void dataDebitMapper(WalletRequest walletRequest, TransferDto transferDto, GameSession gameSession) {

        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setExternalTransactionId(transferDto.getRoundId());
        walletRequest.setRoundId(transferDto.getRoundId());
        walletRequest.setVendorGameCode(transferDto.getGameId());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(transferDto.getVendorBetId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        //walletRequest.setAction("debit");
//        walletRequest.setTakeAll(0);
        BigDecimal amount = transferDto.getBetAmount().abs();
        walletRequest.setTransferAmount(amount);
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
    }

    private void dataCreditMapper(WalletRequest walletRequest, TransferDto transferDto, GameSession gameSession) {

        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        walletRequest.setExternalTransactionId(transferDto.getRoundId());
        walletRequest.setRoundId(transferDto.getRoundId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(transferDto.getVendorBetId());
        //walletRequest.setAction("credit");
        walletRequest.setTakeAll(0);

        BigDecimal amount = transferDto.getBetAmount().abs();

        walletRequest.setTransferAmount(amount);
        walletRequest.setBetAmount(null);

        ResultType resultType = vendorService.calculateResultType(null, amount, transferDto.getJackpotAmount(), false);

        walletRequest.setWinAmount(amount);
        walletRequest.setEffectiveTurnover(BigDecimal.ZERO);
        walletRequest.setJackpotAmount(transferDto.getJackpotAmount());
        walletRequest.setResultType(resultType.code);
        walletRequest.setVendorBetTime(System.currentTimeMillis());
        walletRequest.setVendorSettleTime(System.currentTimeMillis());
    }
}

