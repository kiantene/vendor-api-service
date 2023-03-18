package com.nextgen.gameaggregator.vendor.jdb.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BetNSettleService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;

    public CommonVo betNSettle(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            BetNSettleDto betNSettleDto = HttpService.convertJsonToDto(actionDto.getParams(), BetNSettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(betNSettleDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betNSettleDto.getUid(), betNSettleDto.getMType().toString());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(betNSettleDto, gameSession);

            // 4. Build BetData and WinData
            BetDataDto betDataDto = this.prepareBetData(betNSettleDto);
            WinDataDto winDataDto = this.prepareWinData(betNSettleDto);

            // 5. Send bet request to Operator
            // 5.1 check if player has enough balance
            // 5.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            // 5.3 Process Bet Request
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDataDto, actionDto.getParams());

            // 5.4 Process Bet Result and End Round
            BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDataDto, actionDto.getParams());

            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));

            vo.setStatus("0000");
            vo.setBalance(betResultEvent.getLastBalance());
            vo.setErrText("Succeed");

        } catch (Exception exception) {
            vo.setStatus("9999");
            vo.setBalance(BigDecimal.ZERO);
            vo.setErrText("Failed");
        }

        return vo;
    }

    private void doValidation(BetNSettleDto dto) {
    }

    private void doVerification(BetNSettleDto dto, GameSession gameSession) {
    }

    private BetDataDto prepareBetData(BetNSettleDto dto) {
        BetDataDto betDataDto = new BetDataDto();
        betDataDto.setExternalTransactionId(dto.getGameSeqNo().toString());
        betDataDto.setAmount(dto.getBet());
        betDataDto.setRoundId(dto.getGameSeqNo().toString());
        betDataDto.setGameId(dto.getMType().toString());
        betDataDto.setTimestamp(dto.getTs());
        return betDataDto;
    }

    private WinDataDto prepareWinData(BetNSettleDto dto) {
        WinDataDto winDataDto = new WinDataDto();
        winDataDto.setExternalTransactionId(dto.getGameSeqNo().toString());
        winDataDto.setAmount(dto.getWin());
        winDataDto.setRoundId(dto.getGameSeqNo().toString());
        winDataDto.setGameId(dto.getMType().toString());
        winDataDto.setTimestamp(dto.getTs());
        winDataDto.setEffectiveTurnover(dto.getBet());
        return winDataDto;
    }
}
