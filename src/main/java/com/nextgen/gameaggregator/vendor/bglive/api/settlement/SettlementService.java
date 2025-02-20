package com.nextgen.gameaggregator.vendor.bglive.api.settlement;


import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bglive.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettlementService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorPlayerService vendorPlayerService;
    private final VendorService vendorService;

    @Autowired
    public SettlementService(HttpService httpService,
                             WalletService walletService,
                             GameSessionService gameSessionService,
                             VendorLineService vendorLineService,
                             AgentPlayerService agentPlayerService,
                             VendorPlayerService vendorPlayerService,
                             VendorService vendorService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
        this.vendorService = vendorService;
    }

    public CommonVo settle(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
        try {
            String body = httpRequestLog.getRequestBody();
            SettleDto settleDto = HttpService.convertJsonToDto(body, SettleDto.class);
            // Handle the action and return the resulting value
            this.doValidation(settleDto);

            String vendorPlayerLoginId = settleDto.getParams().getLoginId();
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorPlayerLoginId);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            this.doVerification(settleDto, gameSession);

            if (settleDto.getParams().getOrders() == null || settleDto.getParams().getOrders().isEmpty()) {
                throw new InvalidRequestException("Bet request must contain at least one order.");
            }
            for (OrdersDto order : settleDto.getParams().getOrders()) {
                settleDto.setCurrentOrder(order);
                settleDto.getExternalTransactionId();
                settleDto.getRoundId();
                settleDto.getBetAmount();
                ResultType resultType = vendorService.calculateResultType(settleDto.getBetAmount(), settleDto.getWinAmount(),
                        settleDto.getJackpotAmount(), false);
                walletService.processBetResult(traceId, gameSession, settleDto, resultType, vendorService,
                        httpRequestLog);
            }

            ResultVo resultVo = new ResultVo();
            resultVo.setUserId(vendorPlayer.getId());
            resultVo.setSn(settleDto.getParams().getSn());
            resultVo.setAvailableAmount(walletService.getBalance(traceId, gameSession, httpRequestLog));
            resultVo.setOrderResult("1");
            String tranId = settleDto.getParams().getTranId();
            resultVo.setTranId((tranId == null || tranId.trim().isEmpty()) ? null : tranId);

            commonVo.setSuccessResponse(settleDto.getId(), resultVo);

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

    private void doValidation(SettleDto settleDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(settleDto);

        ParamsDto paramsDto = settleDto.getParams();
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

    private void doVerification(SettleDto settleDto, GameSession gameSession) throws AuthenticationException,
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
        ValidationUtils.isEquals(snCode, settleDto.getParams().getSn(), InvalidPlayerException::new);

        String validateSign = VendorService.encryptBetMd5Key(settleDto.getParams().getRandom(), snCode,
                gameSession.getVendorPlayerUsername(), String.valueOf(settleDto.getParams().getAmount()), secretKey);
        ValidationUtils.isEquals(validateSign, settleDto.getParams().getSign(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}