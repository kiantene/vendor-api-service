package com.nextgen.gameaggregator.vendor.evoplay.api.bet;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DetailsDto;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BetService {

    @Autowired
    private WalletService walletService;

    @Autowired
    private ValidationService validationService;

    public ResponseVo bet(CallbackDto callbackDto, GameSession gameSession, String body, String traceId)
            throws
            CurrencyNotSupportedException,
            InvalidRequestException,
            GameNotSupportedException,
            AuthenticationException,
            InvalidAgentApiCredentialException,
            InsufficientBalanceException,
            InvalidOperatorResponseException,
            CouchbaseDataIntegrityException,
            InvalidPlayerException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException, BetResultIdempotentViolationException, TransactionStillProcessingException {

        callbackDto.getData().setDetailsDto(new Gson().fromJson(callbackDto.getData().getDetails(), DetailsDto.class));
        BetDto betDto = new ModelMapper().map(callbackDto, BetDto.class);

        this.doValidation(betDto);
        this.doVerification(betDto, gameSession);

        BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);

        ResponseDataVo responseDataVo = new ResponseDataVo();
        responseDataVo.setBalance(betEvent.getLastBalance());
        responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());

        ResponseVo responseVo = new ResponseVo();
        responseVo.setData(responseDataVo);

        return responseVo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {

        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getData());
    }

    private void doVerification(BetDto dto, GameSession gameSession)
            throws
            CurrencyNotSupportedException,
            GameNotSupportedException,
            AuthenticationException,
            InvalidPlayerException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException {

        // 2. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getData().getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getData().getDetailsDto().getGame().getGame_id(), GameNotSupportedException::new);
    }

}
