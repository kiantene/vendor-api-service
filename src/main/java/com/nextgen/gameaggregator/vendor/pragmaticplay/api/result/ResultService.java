package com.nextgen.gameaggregator.vendor.pragmaticplay.api.result;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@Slf4j
@RequestScope
public class ResultService {

//    @Autowired
//    public SeamlessBetHistoryRequestRepository seamlessBetHistoryRequestRepository;
//
//    @Autowired
//    public BetHistorySeamlessRequestRepository betHistorySeamlessRequestRepository;
//
//    @Autowired
//    private VendorPlayerAuthenticationRepository vendorPlayerAuthenticationRepository;
//
//    @Autowired
//    private OperatorBetRequestGrpc operatorBetRequestGrpc;
//
//    @Autowired
//    public BetHistorySeamlessOthersRequestRepository betHistorySeamlessOthersRequestRepository;
//
//    @Autowired
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    public SeamlessBetHistoryRequest getSeamlessBetRequestId(ResultDto dto){
//        SeamlessBetHistoryRequest getBetRequest;
//        getBetRequest = this.seamlessBetHistoryRequestRepository.findById("PP_"+dto.getRoundId()).orElse(null);
//
//        return (getBetRequest != null)?getBetRequest:null;
//    }
//
//    public void createRawBetHistorySeamlessOthersCouchBase(String rawRequest, String getVendorCurrencyCode)
//    {
//        String betHistoryId = UUID.randomUUID().toString();
//        Long aggregatorRequestStartMs = Instant.now().toEpochMilli();
//
//        BetHistorySeamlessOthersRequest dataSet = new BetHistorySeamlessOthersRequest(betHistoryId+"_1", "betResult",
//                "SLOT", "PP", getVendorCurrencyCode, rawRequest, aggregatorRequestStartMs.toString(),
//                betHistoryId);
//
//        this.betHistorySeamlessOthersRequestRepository.save(dataSet);
//    }
//
//    public void createRecordToKafkaBetHistoryTopic(String betHistoryId, VendorPlayerAuthentication vpa, String rawRequest)
//    {
//        Long aggregatorRequestStartMs = Instant.now().toEpochMilli();
//
//        BetHistorySeamlessRequest dataSet = new BetHistorySeamlessRequest(betHistoryId+"_1", "betResult", "SLOT",
//                "PP", vpa.getVendorCurrencyCode(), rawRequest, aggregatorRequestStartMs.toString(), betHistoryId);
//
//        Gson gson = new GsonBuilder().create();
//
//        kafkaTemplate.send("topic_seamless_bet_transformation", betHistoryId, gson.toJson(dataSet));
//    }

//    public BigDecimal getBetRequestBalanceFromGRPC(ResultDto dto, String traceId, VendorPlayerAuthentication vpa,
//                                                   SeamlessBetHistoryRequest seamlessBetRequest) {
//        //TODO: call operatorBetRequestGrpc.betRequest to get the balance of player from operator
//        return new BigDecimal("1000");

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
//    }
}
