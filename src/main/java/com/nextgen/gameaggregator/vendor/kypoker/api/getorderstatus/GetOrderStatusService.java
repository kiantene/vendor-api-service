package com.nextgen.gameaggregator.vendor.kypoker.api.getorderstatus;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.*;
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

    public GetOrderStatusService(WalletRequestService walletRequestService,
                                 UnsettledBetService unsettledBetService,
                                 VendorLineService vendorLineService,
                                 SettledBetService settleBetService,
                                 WalletTransactionService walletTransactionService,
                                 VendorPlayerService vendorPlayerService) {

        this.walletRequestService = walletRequestService;
        this.unsettledBetService = unsettledBetService;
        this.vendorLineService = vendorLineService;
        this.settleBetService = settleBetService;
        this.walletTransactionService = walletTransactionService;
        this.vendorPlayerService = vendorPlayerService;
    }

    public CommonVo getOrderStatus(String actionDto, String traceId, HttpRequestLog httpRequestLog, String decryptedParam, Long timeStamp) {
        // Construct VO
        CommonVo vo = new CommonVo();
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        UnsettledBet unsettledBet;
        VendorPlayer vendorPlayer = null;
        Integer vendorId = 0;
        String externalTransactionId = null;

        try {

            GetOrderStatusDto getOrderStatusDto = HttpService.convertQueryStringToDto(decryptedParam, GetOrderStatusDto.class);

            doValidation(getOrderStatusDto);

            GetOrderStatusAgentDto getOrderStatusAgentDto = HttpService.convertQueryStringToDto(actionDto, GetOrderStatusAgentDto.class);

            externalTransactionId = getOrderStatusDto.getOrderId();

            vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(getOrderStatusDto.getAccount());

            vendorId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.AGENT_ID,getOrderStatusAgentDto.getAgent());

            unsettledBetService.getByVendorIdAndExternalTransactionId(vendorId, getOrderStatusDto.getOrderId());

            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(ResponseCodes.CODE3);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_ORDER_STATUS);
            vo.setD(d);

        } catch (BetNotFoundException e){
            try {
                settleBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayer.getId(),externalTransactionId);

                ResponseObjectDto d = new ResponseObjectDto();
                d.setCode(ResponseCodes.SUCCESS);
                d.setStatus(ResponseCodes.STATUS_SUCCESS);
                vo.setM(EndPoints.LAUNCH_GAME);
                vo.setS(ResponseCodes.GET_ORDER_STATUS);
                vo.setD(d);

            } catch (BetNotFoundException ex) {
                {
                    walletTransactionService.getByVendorIdAndExternalTransactionId(vendorId,externalTransactionId);

                    ResponseObjectDto d = new ResponseObjectDto();
                    d.setCode(ResponseCodes.SUCCESS);
                    d.setStatus(ResponseCodes.STATUS_SUCCESS);
                    vo.setM(EndPoints.LAUNCH_GAME);
                    vo.setS(ResponseCodes.GET_ORDER_STATUS);
                    vo.setD(d);

                }
            }

        } catch (Exception e){
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(ResponseCodes.INTERNAL_ERROR);
            d.setStatus(ResponseCodes.CODE2);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_ORDER_STATUS);
            vo.setD(d);

        }finally {
            walletRequestService.end(walletRequest, httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(GetOrderStatusDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

}
