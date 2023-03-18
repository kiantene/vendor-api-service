package com.nextgen.gameaggregator.vendor.jdb.api.balance;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BalanceService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;

    public CommonVo balance(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            BalanceDto balanceDto = HttpService.convertJsonToDto(actionDto.getParams(), BalanceDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // 2. Get vendor player details
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(balanceDto.getUid());
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // 4. Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // 5. Get vendor line supported currency
            VendorLine vendorLine = vendorLineService.getVendorLineById(vendorPlayer.getVendorLineId());

            // Construct VO
            vo.setBalance(balance);
            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (Exception exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(BalanceDto dto) {
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) {
    }
}
