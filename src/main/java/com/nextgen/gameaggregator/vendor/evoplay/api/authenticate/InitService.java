package com.nextgen.gameaggregator.vendor.evoplay.api.authenticate;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evoplay.api.balanceIncrease.BalanceService;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class InitService {
    @Value("${vendor.evoplay.isBalancedIncreaseTestEnabled:false}")
    private Boolean isBalancedIncreaseTestEnabled = false;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    public ResponseVo init(CallbackDto callbackDto, GameSession gameSession, String traceId) throws
            InvalidAgentApiCredentialException,
            InvalidOperatorResponseException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException,
            InvalidRequestException,
            VendorCurrencyNotSupportException {

        this.doValidation(callbackDto);
        this.doVerification(gameSession);

        // Retrieve the latest wallet balance from Operator
        BigDecimal balance = null;
        if (isBalancedIncreaseTestEnabled) {
            balance = balanceService.getBalance(gameSession.getVendorPlayerUsername(), traceId, gameSession, httpRequestLog);
        } else {
            balance = walletService.getBalance(traceId, gameSession);
        }

        ResponseDataVo responseDataVo = new ResponseDataVo();
        responseDataVo.setBalance(balance);
        responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());

        ResponseVo responseVo = new ResponseVo();
        responseVo.setData(responseDataVo);

        return responseVo;
    }

    private void doValidation(CallbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException {

        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
