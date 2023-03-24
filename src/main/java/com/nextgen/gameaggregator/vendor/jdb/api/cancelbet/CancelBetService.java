package com.nextgen.gameaggregator.vendor.jdb.api.cancelbet;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.eventing.events.BetRefundEvent;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CancelBetService {

    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;

    public CommonVo cancelBet(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            CancelBetDto cancelBetDto = HttpService.convertJsonToDto(actionDto.getParams(), CancelBetDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(cancelBetDto);

            // 2. Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(cancelBetDto.getUid());
            BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionId(cancelBetDto.getTransferId(), vendorPlayer.getVendorId());

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betHistory.getGameSessionToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(cancelBetDto, gameSession);

            // 5. Send refund to Operator
            BetRefundEvent betRefundEvent = walletService.processRefund(traceId, cancelBetDto.getTransferId(), gameSession, actionDto.getParams());

            vo.setBalance(betRefundEvent.getLastBalance());
            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (Exception exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(CancelBetDto dto) {

    }

    private void doVerification(CancelBetDto dto, GameSession gameSession) {

    }
}
