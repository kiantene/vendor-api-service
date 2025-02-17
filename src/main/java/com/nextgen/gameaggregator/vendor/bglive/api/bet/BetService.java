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
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bglive.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BetService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorPlayerService vendorPlayerService;

    @Autowired
    public BetService(HttpService httpService,
                      WalletService walletService,
                      GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      AgentPlayerService agentPlayerService, VendorPlayerService vendorPlayerService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
    }

    public CommonVo bet(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
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
                betDto.setCurrentOrder(order);
                betDto.getExternalTransactionId();
                betDto.getRoundId();
                betDto.getBetAmount();
                walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
            }

            ResultVo resultVo = new ResultVo();
            resultVo.setUserId(vendorPlayer.getId());
            resultVo.setSn(betDto.getParams().getSn());
            resultVo.setAmount(walletService.getBalance(traceId, gameSession, httpRequestLog));
            resultVo.setOrderResult("1");
            String tranId = betDto.getParams().getTranId();
            resultVo.setTranId(tranId != null && !tranId.trim().isEmpty() ? tranId : null);
            commonVo.setSuccessResponse(betDto.getId(), resultVo);

        } catch (InsufficientBalanceException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.INSUFFICIENT_BALANCE.code,
                    ResponseCodes.INSUFFICIENT_BALANCE.message, ResponseCodes.INSUFFICIENT_BALANCE.message);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidPlayerException e) {

            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.PLAYER_INVALID.code,
                    ResponseCodes.PLAYER_INVALID.message, ResponseCodes.PLAYER_INVALID.message);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException e) {

            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.AUTH_INVALID.code,
                    ResponseCodes.AUTH_INVALID.message, ResponseCodes.AUTH_INVALID.message);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);

        }
        return commonVo;
    }

    private void doValidation(BetDto betDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(betDto);

        ParamsDto paramsDto = betDto.getParams();
        if (paramsDto != null) {
            ValidationUtils.validateRequest(paramsDto);

            List<OrdersDto> ordersList = paramsDto.getOrders();
            if (ordersList != null) {
                for (OrdersDto order : ordersList) {
                    ValidationUtils.validateRequest(order);
                }
            }
        }

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

        String validateSign = VendorService.encryptBetMd5Key(betDto.getParams().getRandom(), snCode,
                gameSession.getVendorPlayerUsername(), String.valueOf(betDto.getParams().getAmount()), secretKey);
        ValidationUtils.isEquals(validateSign, betDto.getParams().getSign(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}
