package com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@Slf4j
@RequestScope
public class BetService {
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

//    @Autowired
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    public String getSeamlessBetRequestId(BetDto dto){
//        SeamlessBetHistoryRequest getBetRequest;
//        getBetRequest = this.seamlessBetHistoryRequestRepository.findById("PP_"+dto.getRoundId()).orElse(null);
//
//        return (getBetRequest != null)?getBetRequest.getBetHistoryId():null;
//    }
//
//    public String createLogSeamlessBetHistoryRequest(BetDto dto, String rawRequest) throws CreateLogSeamlessBetHistoryException {
//        SeamlessBetHistoryRequest dataSet = new SeamlessBetHistoryRequest("PP_"+dto.getRoundId(), dto.getReference(),
//                UUID.randomUUID().toString(), dto.getRoundId(), Double.parseDouble(dto.getAmount()), dto.getTimestamp(),
//                Instant.now().toEpochMilli(),"betRequest", "SLOT", rawRequest, "PP");
//
//        String getBetHistoryId = seamlessBetHistoryRequestRepository.save(dataSet).getBetHistoryId();
//
//        if (getBetHistoryId == null) {
//            throw new CreateLogSeamlessBetHistoryException();
//        }
//
//        return getBetHistoryId;
//    }
//
//    public void createRawBetHistorySeamlessRequest(String betHistoryId, VendorPlayerAuthentication vpa, String rawRequest)
//            throws CreateRawBetHistorySeamlessException {
//
//        Long aggregatorRequestStartMs = Instant.now().toEpochMilli();
//
//        BetHistorySeamlessRequest dataSet = new BetHistorySeamlessRequest(betHistoryId, "betRequest", "",
//                "PP", vpa.getVendorCurrencyCode(), rawRequest, aggregatorRequestStartMs.toString(), betHistoryId);
//
//        String getBetHistoryId = betHistorySeamlessRequestRepository.save(dataSet).getBetHistoryId();
//
//        if (getBetHistoryId == null) {
//            throw new CreateRawBetHistorySeamlessException();
//        }
//    }
//
//    public void createRecordToKafkaBetHistoryTopic(String betHistoryId, VendorPlayerAuthentication vpa, String rawRequest)
//    {
//        Long aggregatorRequestStartMs = Instant.now().toEpochMilli();
//
//        BetHistorySeamlessRequest dataSet = new BetHistorySeamlessRequest(betHistoryId, "betRequest", "",
//                "PP", vpa.getVendorCurrencyCode(), rawRequest, aggregatorRequestStartMs.toString(), betHistoryId);
//
//        Gson gson = new GsonBuilder().create();
//
//        kafkaTemplate.send("topic_seamless_bet_transformation", betHistoryId, gson.toJson(dataSet));
//    }

//    public BigDecimal getBetRequestBalanceFromGRPC(BetDto dto, String traceId, VendorPlayerAuthentication vpa, String seamlessBetRequestId) {
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
