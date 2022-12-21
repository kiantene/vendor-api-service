package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.bet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.grpc.v1.operator.betrequest.BetRequestGrpcVo;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.AgentCredentialReaderManager;
import com.nextgen.gameaggregator.vendor.exception.*;
import com.nextgen.gameaggregator.vendor.grpc.v1.subcriber.OperatorBetRequestGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.VENDOR_CODE;

@Service
@Slf4j
@RequestScope
public class BetService {

    @Autowired
    public SeamlessBetHistoryRequestRepository seamlessBetHistoryRequestRepository;

    @Autowired
    public BetHistorySeamlessRequestRepository betHistorySeamlessRequestRepository;

    @Autowired
    private VendorPlayerAuthenticationRepository vendorPlayerAuthenticationRepository;

    @Autowired
    private OperatorBetRequestGrpc operatorBetRequestGrpc;

    @Autowired
    private AgentCredentialReaderManager agentCredentialReaderManager;

    public <T> T queryStringToDto(String queryString, Class<T> clazz) {

        log.info(queryString);

        HashMap<String, Object> queryParameterMap = new HashMap<String, Object>();
        String[] fields = queryString.split("&");

        for (int i = 0; i < fields.length; ++i) {
            String[] kv = fields[i].split("=");
            if (2 == kv.length) {
                queryParameterMap.put(kv[0], kv[1]);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        T t = mapper.convertValue(queryParameterMap, clazz);

        return t;
    }

    public void validateRequest(BetDto dto) throws InvalidRequestException {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        if (!validator.validate(dto).isEmpty()) { // Missing request parameters
            throw new InvalidRequestException();
        }
    }

    public void validateHash(String hash, String requestBody) throws InvalidHashException {
        if (hash.compareTo("%20") == 0) {
            throw new InvalidHashException();
        }
    }

    public String getSeamlessBetRequestId(BetDto dto){
        SeamlessBetHistoryRequest getBetRequest;
        getBetRequest = this.seamlessBetHistoryRequestRepository.findById("PP_"+dto.getRoundId()).orElse(null);

        return (getBetRequest != null)?getBetRequest.getBetHistoryId():null;
    }

    public String createLogSeamlessBetHistoryRequest(BetDto dto, String rawRequest) throws CreateLogSeamlessBetHistoryException {
        SeamlessBetHistoryRequest dataSet = new SeamlessBetHistoryRequest("PP_"+dto.getRoundId(), dto.getReference(),
                UUID.randomUUID().toString(), dto.getRoundId(), Double.parseDouble(dto.getAmount()), dto.getTimestamp(),
                Instant.now().toEpochMilli(),"betRequest", "SLOT", rawRequest, "PP");

        String getBetHistoryId = seamlessBetHistoryRequestRepository.save(dataSet).getBetHistoryId();

        if (getBetHistoryId == null) {
            throw new CreateLogSeamlessBetHistoryException();
        }

        return getBetHistoryId;
    }

    public void createRawBetHistorySeamlessRequest(String betHistoryId, VendorPlayerAuthentication vpa, String rawRequest)
            throws CreateRawBetHistorySeamlessException {

        Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

        BetHistorySeamlessRequest dataSet = new BetHistorySeamlessRequest(betHistoryId, "betRequest", "",
                "PP", vpa.getVendorCurrencyCode(), rawRequest, aggregatorRequestStartMs.toString(), betHistoryId);

        String getBetHistoryId = betHistorySeamlessRequestRepository.save(dataSet).getBetHistoryId();

        if (getBetHistoryId == null) {
            throw new CreateRawBetHistorySeamlessException();
        }
    }

    public VendorPlayerAuthentication verifyToken(String token) throws AuthenticationException {
        VendorPlayerAuthentication authenticatedUser = new VendorPlayerAuthentication();

        authenticatedUser = vendorPlayerAuthenticationRepository.findByTraceId(token);

        Optional.ofNullable(authenticatedUser).orElseThrow(AuthenticationException::new);

        return authenticatedUser;
    }

    public BigDecimal getBetRequestBalanceFromGRPC(BetDto dto, String traceId, VendorPlayerAuthentication vpa, String seamlessBetRequestId) {
        //TODO: call operatorBetRequestGrpc.betRequest to get the balance of player from operator
        return new BigDecimal("1000");

        //prepare call to operator grpc
//        BetRequestGrpcVo serviceVo = this.operatorBetRequestGrpc.betRequest(
//                vpa.getAgentId(),
//                vpa.getAgentPlayerId(),
//                vpa.getGameId(),
//                vpa.getCurrencyCode(),
//                traceId,
//                agentCredentialReaderManager.findByAgentId(vpa.getAgentId()).getId(),
//                seamlessBetRequestId,
//                dto.getReference(),
//                dto.getRoundId(),
//                Double.parseDouble(dto.getAmount()),
//                dto.getTimestamp()
//        );
    }
}
