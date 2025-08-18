//package com.nextgen.gameaggregator.vendor.crystal.api.settle;
//
//import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
//import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
//import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
//import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
//import com.nextgen.gameaggregator.core.engine.wallet.result.SettleType;
//import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
////@RequestMapping(path = EndPoints.PATH)
//@RequiredArgsConstructor
//public class SettleController {
//
//    private final WalletBetService walletService;
//    private final SettleRequestMapper settleRequestMapper;
//    private final SettleResponseMapper settleResponseMapper;
//
//    @PostMapping(path = EndPoints.SETTLE)
//    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
//    public ResponseEntity<SettleResponse> doSettle(
//            @Valid @RequestBody SettleRequest request) {
//
//        BetResultContext context = settleRequestMapper.toBetResultContext(request);
//        PlayerBalanceData balanceData = walletService
//                .initialise(context)
//                .configure(config -> config.setSettleType(SettleType.BET))
//                .isBetTxn(false)
//                .process();
//        return ResponseEntity.ok(settleResponseMapper.toVendor(context, balanceData));
//    }
//}
