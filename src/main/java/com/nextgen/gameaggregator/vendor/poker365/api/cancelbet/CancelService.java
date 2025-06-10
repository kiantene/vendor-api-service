package com.nextgen.gameaggregator.vendor.poker365.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.BetStatus;
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

import java.math.BigDecimal;
import java.util.Objects;

@Service
@Slf4j
public class CancelService {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final VendorPlayerService vendorPlayerService;
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;
    private final WalletTransactionService walletTransactionService;

    @Autowired
    public CancelService(HttpService httpService,
                         GameSessionService gameSessionService,
                         AgentPlayerService agentPlayerService,
                         VendorLineService vendorLineService,
                         RequestIdempotentLogService requestIdempotentLogService,
                         VendorPlayerService vendorPlayerService,
                         WalletRequestService walletRequestService,
                         OperatorWalletService operatorWalletService,
                         WalletTransactionService walletTransactionService) {
        this.httpService = httpService;
        this.requestIdempotentLogService = requestIdempotentLogService;
        this.gameSessionService = gameSessionService;
        this.agentPlayerService = agentPlayerService;
        this.vendorLineService = vendorLineService;
        this.vendorPlayerService = vendorPlayerService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
        this.walletTransactionService = walletTransactionService;
    }

    private void dataMapper(WalletRequest walletRequest, MessageDto dto, GameSession gameSession) {
        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setExternalTransactionId(dto.getGameNumber());
        walletRequest.setRoundId(dto.getRoundId());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        walletRequest.setVendorId(gameSession.getVendorId());
        walletRequest.setWinAmount(BigDecimal.ZERO);
        walletRequest.setEffectiveTurnover(BigDecimal.ZERO);
        walletRequest.setJackpotAmount(BigDecimal.ZERO);
        walletRequest.setBetAmount(BigDecimal.ZERO);
        walletRequest.setVendorBetTime(System.currentTimeMillis());
        walletRequest.setJackpotAmount(BigDecimal.ZERO);
        walletRequest.setWinLoss(BigDecimal.ZERO);

    }

    public CommonVo cancel(HttpRequestLog httpRequestLog, String traceId) throws JsonProcessingException {
        CommonVo commonVo = new CommonVo();
        BigDecimal balance;
        WalletTransaction walletTransaction = null;
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        boolean isRequestExists = false;
        Integer vendorPlayerId;

        try {
            String body = httpRequestLog.getRequestBody();

            CommonDto commonDto = VendorService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);

            String formatedMessageDto = commonDto.getMessage();

            MessageDto messageDto = HttpService.convertJsonToDto(formatedMessageDto, MessageDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(commonDto, messageDto);

            vendorPlayerId = Integer.valueOf(messageDto.getUserId());

            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(vendorPlayerId), null);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, messageDto, gameSession);

            walletTransaction = walletTransactionService.getByRoundIdAndVendorPlayerUsername(messageDto.getRoundId(), gameSession.getVendorPlayerUsername());

            if(walletTransaction == null) {
                throw new BetNotFoundException();

            } else if((Objects.equals(walletTransaction.getAction(), "credit") && walletTransaction.getOperatorStatus() == 1)){
                throw new BetResultIdempotentViolationException();

            } else {

                this.dataMapper(walletRequest, messageDto, gameSession);

                walletRequest.setTransferAmount(walletTransaction.getTransferAmount());

                walletRequest.setBetAmount(walletTransaction.getTransferAmount());

                walletRequest.setVendorBetId(walletTransaction.getBetId());

                walletRequest.setExternalTransactionId(walletTransaction.getBetId());

                walletRequest.setBetStatus(BetStatus.REFUNDED);

                walletRequest = operatorWalletService.betCredit(walletRequest);

                commonVo.setBalance(walletRequest.getBalanceAfter());

                commonVo.setStatus(ResponseCodes.SUCCESS_200.status);

             }

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
            walletRequestService.end(walletRequest, httpRequestLog,commonVo);

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
            InvalidVendorLineException,
            InvalidPlayerException,
            CredentialNotFoundException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());

        Integer vendorLineId = vendorLine.getId();

        String cert = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CERT);

        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(cert, commonDto.getKey(), AuthenticationException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerId()), messageDto.getUserId(), InvalidPlayerException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}
