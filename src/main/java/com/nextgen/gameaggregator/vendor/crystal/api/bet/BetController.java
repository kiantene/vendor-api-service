package com.nextgen.gameaggregator.vendor.crystal.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class BetController {
    private final WalletBetService walletService;
    private final BetRequestMapper requestMapper;
    private final BetResponseMapper responseMapper;

    @PostMapping(path = EndPoints.BET)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<BetResponse> doBet(
            @Valid @RequestBody BetRequest request) {

        BetContext context = requestMapper.toBetContext(request);
        PlayerBalanceData balanceData = walletService.process(context);
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }
}
