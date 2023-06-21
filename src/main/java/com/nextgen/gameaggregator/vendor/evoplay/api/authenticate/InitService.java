package com.nextgen.gameaggregator.vendor.evoplay.api.authenticate;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class InitService {
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    public ResponseVo init(CallbackDto callbackDto, GameSession gameSession, String traceId, String key) throws
            InvalidAgentApiCredentialException,
            InvalidOperatorResponseException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException,
            InvalidRequestException {

        this.doValidation(callbackDto);
        this.doVerification(callbackDto, gameSession, key);

        // Retrieve the latest wallet balance from Operator
        BigDecimal balance = walletService.getBalance(traceId, gameSession);

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

    private void doVerification(CallbackDto callbackDto, GameSession gameSession, String key)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            AuthenticationException {

        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify Signature
        String signature = VendorService.generateSignature(callbackDto, key);
        ValidationUtils.isEquals(signature, callbackDto.getSignature(), AuthenticationException::new);
    }
}
