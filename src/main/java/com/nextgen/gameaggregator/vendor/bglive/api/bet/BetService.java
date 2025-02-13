package com.nextgen.gameaggregator.vendor.bglive.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BetService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final ValidationService validationService;
    private final VendorPlayerService vendorPlayerService;

    @Autowired
    public BetService(ValidationService validationService,
                      HttpService httpService,
                      VendorService vendorService,
                      WalletService walletService,
                      GameSessionService gameSessionService,
                      VendorGameService vendorGameService,
                      VendorLineService vendorLineService,
                      AgentPlayerService agentPlayerService, VendorPlayerService vendorPlayerService) {
        this.validationService = validationService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorGameService = vendorGameService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
    }

    public BetVo bet(HttpRequestLog httpRequestLog, String traceId) {
        BetVo betVo = new BetVo();
        try {
            String body = httpRequestLog.getRequestBody();
            BetDto betDto = HttpService.convertJsonToDto(body, BetDto.class);
            // Handle the action and return the resulting value
            this.doValidation(betDto);

            String vendorPlayerLoginId = betDto.getParams().getLoginId();
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorPlayerLoginId);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());
            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            if (betDto.getParams().getOrders() == null || betDto.getParams().getOrders().isEmpty()) {
                throw new InvalidRequestException("Bet request must contain at least one order.");
            }
            for (OrdersDto order : betDto.getParams().getOrders()) {
                walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
            }

            // set getbalanceVo
            betVo.setUserId(vendorPlayer.getId());
            betVo.setSn(betDto.getParams().getSn());
            betVo.setAmount(walletService.getBalance(traceId, gameSession, httpRequestLog));
            betVo.setOrderResult("1");
            String tranId = betDto.getParams().getTranId();
            betVo.setTranId((tranId == null || tranId.trim().isEmpty()) ? null : tranId);

            betVo.setSuccessResponse(betDto.getId(), betVo);
//        } catch (InvalidAgentApiCredentialException |
//                 DisabledAgentPlayerException |
//                 DisabledGameException |
//                 InvalidRequestException |
//                 JsonProcessingException |
//                 TransactionStillProcessingException |
//                 InsufficientBalanceException |
//                 DisabledVendorLineException e) {
//            //set Vo
//            vo.setErrorResponse(ResponseCodes.INVALID_DATA);
//            httpService.logError(httpRequestLog, e);
//
//        } catch (AuthenticationException |
//                 InvalidPlayerException |
//                 VendorCurrencyNotSupportException |
//                 CurrencyNotSupportedException |
//                 GameNotSupportedException e) {
//
//            vo.setErrorResponse(ResponseCodes.INVALID_SESSION);
//            httpService.logError(httpRequestLog, e);
//
//        } catch (BetResultIdempotentViolationException e) {
//
//            vo.setSuccessResponse(vendorService.getCurrentBalance(traceId, gameSession, httpRequestLog));
//            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            betVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);

        }
        return betVo;
    }

    private void doValidation(BetDto betDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(betDto);

    }

    private void doVerification(BetDto betDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            InvalidVendorLineException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidFormatException {

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String snCode = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.SN_CODE);
        String secretKey = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.API_KEY);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(snCode, betDto.getParams().getSn(), InvalidPlayerException::new);

        String validateSign = VendorService.encryptLoginMd5Key(betDto.getParams().getRandom(), snCode,
                gameSession.getVendorPlayerUsername(), secretKey);
        ValidationUtils.isEquals(validateSign, betDto.getParams().getSign(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}
