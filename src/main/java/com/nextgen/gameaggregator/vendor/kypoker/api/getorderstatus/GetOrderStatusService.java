package com.nextgen.gameaggregator.vendor.kypoker.api.getorderstatus;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.vo.ResponseObjectDto;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;

@Service
public class GetOrderStatusService {

    private final UnsettledBetService unsettledBetService;
    private final SettledBetService settleBetService;
    private final WalletTransactionService walletTransactionService;
    private final GameSessionService gameSessionService;
    private final HttpService httpService;


    public GetOrderStatusService(UnsettledBetService unsettledBetService,
                                 SettledBetService settleBetService,
                                 WalletTransactionService walletTransactionService,
                                 GameSessionService gameSessionService,
                                 HttpService httpService) {

        this.unsettledBetService = unsettledBetService;
        this.settleBetService = settleBetService;
        this.walletTransactionService = walletTransactionService;
        this.gameSessionService = gameSessionService;
        this.httpService = httpService;
    }

    public CommonVo getOrderStatus(HttpRequestLog httpRequestLog, String decryptedParam) {
        // Construct VO
        CommonVo vo = new CommonVo();
        String externalTransactionId = null;
        ResponseObjectDto d = new ResponseObjectDto();
        GameSession gameSession = null;
        String traceId = httpRequestLog.getId();

        try {

            GetOrderStatusDto getOrderStatusDto = HttpService.convertQueryStringToDtoUrlDecode(decryptedParam, GetOrderStatusDto.class);

            doValidation(getOrderStatusDto);

            gameSession = this.verifyGameSessionToken(getOrderStatusDto.getAccount(), traceId); //token check

            this.doVerification(getOrderStatusDto, gameSession);

            externalTransactionId = getOrderStatusDto.getOrderId();

            unsettledBetService.getByVendorIdAndExternalTransactionId(gameSession.getVendorId(), getOrderStatusDto.getOrderId());

            d.setCode(ResponseCodes.STATUS_PROCESSING);

        } catch (BetNotFoundException e) {
            try {
                settleBetService.getByVendorPlayerIdAndExternalTransactionId(gameSession.getVendorPlayerId(), externalTransactionId);
                d.setCode(ResponseCodes.SUCCESS);
                d.setStatus(ResponseCodes.STATUS_SUCCESS);

            } catch (BetNotFoundException ex) {

                WalletTransaction walletTransaction = walletTransactionService.getByVendorIdAndExternalTransactionId(gameSession.getVendorId(), externalTransactionId);

                //GA-12953 order status bet not found should response failed to vendor, to trigger vendor resend credit endpoint
                //remap to status bet not found
                if (walletTransaction == null) {
                    d.setCode(ResponseCodes.BET_NOT_FOUND);
                    d.setStatus(ResponseCodes.STATUS_BET_NOT_FOUND);
                    httpService.logError(httpRequestLog, e);
                } else {
                    d.setCode(ResponseCodes.SUCCESS);
                    d.setStatus(ResponseCodes.STATUS_SUCCESS);
                }

            }

        } catch (InvalidRequestException invalidRequestException) {
            d.setCode(ResponseCodes.INVALID_REQUEST);
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (Exception e) {
            d.setCode(ResponseCodes.INTERNAL_ERROR);
            d.setStatus(ResponseCodes.STATUS_FAILED);
            httpService.logError(httpRequestLog, e);

        } finally {
            vo.setM(EndPoints.API_ENDPOINT);
            vo.setS(ResponseCodes.GET_ORDER_STATUS);
            vo.setD(d);
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }

    private void doValidation(GetOrderStatusDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GetOrderStatusDto dto, GameSession gameSession) throws
            InvalidRequestException {

        // Verify vendor gameCode, currency and platform
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAccount(), InvalidRequestException::new);

    }

    private GameSession verifyGameSessionToken(String vendorPlayerUsername, String traceId) throws GameNotSupportedException, InvalidPlayerException, VendorCurrencyNotSupportException {
        GameSession gameSession;
        try {
            gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(vendorPlayerUsername);
            if (gameSession == null) {
                throw new AuthenticationException("token expired");
            }
        } catch (AuthenticationException e) {
            gameSession = gameSessionService.generateNewSessionToken(vendorPlayerUsername);
            //no need game code.
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(traceId);
            gameSession.setVendorToken(traceId);
        }
        //no regenerate game code logic.
        return gameSession;
    }
}
