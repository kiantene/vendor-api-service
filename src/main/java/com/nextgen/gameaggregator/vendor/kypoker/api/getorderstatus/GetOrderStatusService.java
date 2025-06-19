package com.nextgen.gameaggregator.vendor.kypoker.api.getorderstatus;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.vendor.kypoker.api.cancel.CancelDto;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.vo.ResponseObjectDto;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.kypoker.constant.*;

@Service
public class GetOrderStatusService {

    private final WalletRequestService walletRequestService;
    private final UnsettledBetService unsettledBetService;
    private final VendorLineService vendorLineService;
    private final SettledBetService settleBetService;
    private final WalletTransactionService walletTransactionService;
    private final VendorPlayerService vendorPlayerService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;

    public GetOrderStatusService(WalletRequestService walletRequestService,
                                 UnsettledBetService unsettledBetService,
                                 VendorLineService vendorLineService,
                                 SettledBetService settleBetService,
                                 WalletTransactionService walletTransactionService,
                                 VendorPlayerService vendorPlayerService,
                                 ValidationService validationService,
                                 GameSessionService gameSessionService) {

        this.walletRequestService = walletRequestService;
        this.unsettledBetService = unsettledBetService;
        this.vendorLineService = vendorLineService;
        this.settleBetService = settleBetService;
        this.walletTransactionService = walletTransactionService;
        this.vendorPlayerService = vendorPlayerService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
    }

    public CommonVo getOrderStatus(String actionDto, String traceId, HttpRequestLog httpRequestLog, String decryptedParam, Long timeStamp) {
        // Construct VO
        CommonVo vo = new CommonVo();
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        VendorPlayer vendorPlayer = null;
        Integer vendorId = 0;
        String externalTransactionId = null;
        ResponseObjectDto d = new ResponseObjectDto();
        GameSession gameSession;

        try {

            GetOrderStatusDto getOrderStatusDto = HttpService.convertQueryStringToDtoUrlDecode(decryptedParam, GetOrderStatusDto.class);

            doValidation(getOrderStatusDto);

            GetOrderStatusAgentDto getOrderStatusAgentDto = HttpService.convertQueryStringToDtoUrlDecode(actionDto, GetOrderStatusAgentDto.class);

            externalTransactionId = getOrderStatusDto.getOrderId();

            vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(getOrderStatusDto.getAccount());

            gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(getOrderStatusDto.getAccount()); //token check

            vendorId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.AGENT_ID,getOrderStatusAgentDto.getAgent());

            unsettledBetService.getByVendorIdAndExternalTransactionId(vendorId, getOrderStatusDto.getOrderId());

            this.doVerification(getOrderStatusDto, gameSession);

            d.setCode(ResponseCodes.STATUS_PROCESSING);

        } catch (BetNotFoundException e){
            try {
                settleBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayer.getId(),externalTransactionId);
                d.setCode(ResponseCodes.SUCCESS);
                d.setStatus(ResponseCodes.STATUS_SUCCESS);

            } catch (BetNotFoundException ex) {
                {
                    walletTransactionService.getByVendorIdAndExternalTransactionId(vendorId,externalTransactionId);
                    d.setCode(ResponseCodes.SUCCESS);
                    d.setStatus(ResponseCodes.STATUS_SUCCESS);

                }
            }

        } catch (Exception e){
            d.setCode(ResponseCodes.INTERNAL_ERROR);
            d.setStatus(ResponseCodes.STATUS_FAILED);

        }finally {
            vo.setM(EndPoints.API_ENDPOINT);
            vo.setS(ResponseCodes.GET_ORDER_STATUS);
            vo.setD(d);
            walletRequestService.end(walletRequest, httpRequestLog, vo);

        }

        return vo;
    }

    private void doValidation(GetOrderStatusDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GetOrderStatusDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            AuthenticationException,
            InvalidRequestException {

        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getAccount());

        // Verify vendor gameCode, currency and platform
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAccount(), InvalidRequestException::new);

    }

}
