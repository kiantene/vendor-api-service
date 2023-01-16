package com.nextgen.gameaggregator.vendor.pgsoft.api.result;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.service.BetHistoryService;
import com.nextgen.gameaggregator.entity.VendorGame;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultService {
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private BetHistoryService betHistoryService;

    public BetResultEvent process(String traceId, GameSession gameSession, String body) throws
            InvalidRequestException, BetNotFoundException,
            InvalidOperatorResponseException, DuplicateExternalTransactionIdException {
        // Construct result dto
        ResultDto dto = HttpService.convertQueryStringToDto(body, ResultDto.class);
        return walletService.processWin(traceId, gameSession, dto, body);
    }

    public Boolean shouldReprocess(CashTransferInOutDto dto) throws InvalidPlayerException, GameNotSupportedException {
        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(dto.getPlayerName());
        VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(dto.getGameId(), 3);
        Boolean shouldReprocess = false;
        try {
            betHistoryService.checkDuplicateExternalTransaction(dto.getExternalTransactionId(), vendorGame.getId(), vendorPlayer.getId());
        } catch (DuplicateExternalTransactionIdException e) {
            shouldReprocess = true;
        }
        return shouldReprocess;
    }

}
