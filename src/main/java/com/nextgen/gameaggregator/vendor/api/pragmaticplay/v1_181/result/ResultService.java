package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorCredentialReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorCredentialValueReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.AgentCredentialReaderManager;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorCredentialReaderManager;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorCredentialValueReaderManager;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.vendor.grpc.v1.subcriber.OperatorBetRequestGrpc;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequestScope
public class ResultService {

    @Autowired
    public VendorCredentialReaderManager vendorCredentialReaderManager;

    @Autowired
    public VendorCredentialValueReaderManager vendorCredentialValueReaderManager;
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

    @Autowired
    public BetHistorySeamlessOthersRequestRepository betHistorySeamlessOthersRequestRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

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

    public void validateRequest(ResultDto dto, Class<ResultDto> clazz) throws InvalidRequestException {
        Map<String, String> validationMap = new HashMap<String, String>();

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Set<ConstraintViolation<ResultDto>> violations = validator.validate(dto);
        for (ConstraintViolation<ResultDto> violation : violations) {
            validationMap.put(violation.getPropertyPath().toString(), violation.getPropertyPath() + " " + violation.getMessage());
        }

        System.out.println("validationMap++++ "+validationMap);

        if(!validationMap.isEmpty()){
            throw new InvalidRequestException(validationMap.toString());
        }
    }

    public void validateHash(String hash, String secretKey, String requestBody) throws InvalidSignatureException {

        String modifiedQueryString = requestBody.replaceAll("(^|&)hash=.*?(&|$)", "$1$2");

        MultiValueMap<String, String> map = UriComponentsBuilder.newInstance()
                .query(modifiedQueryString)
                .build()
                .getQueryParams();

        String checkerHash = this.generateHash(map, secretKey);

        if(!hash.equals(checkerHash)){
            throw new InvalidSignatureException();
        }

    }

    private String generateHash(MultiValueMap<String, String> params, String secret) {
        String payload = params.keySet().stream()
                .sorted()
                .map(key -> key + "=" + params.get(key).get(0))
                .collect(Collectors.joining("&"));

        payload += secret;
        return DigestUtils.md5Hex(payload);
    }

    public SeamlessBetHistoryRequest getSeamlessBetRequestId(ResultDto dto){
        SeamlessBetHistoryRequest getBetRequest;
        getBetRequest = this.seamlessBetHistoryRequestRepository.findById("PP_"+dto.getRoundId()).orElse(null);

        return (getBetRequest != null)?getBetRequest:null;
    }

    public void createRawBetHistorySeamlessOthersCouchBase(String rawRequest, String getVendorCurrencyCode)
    {
        String betHistoryId = UUID.randomUUID().toString();
        Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

        BetHistorySeamlessOthersRequest dataSet = new BetHistorySeamlessOthersRequest(betHistoryId+"_1", "betResult",
                "SLOT", "PP", getVendorCurrencyCode, rawRequest, aggregatorRequestStartMs.toString(),
                betHistoryId);

        this.betHistorySeamlessOthersRequestRepository.save(dataSet);
    }

    public void createRecordToKafkaBetHistoryTopic(String betHistoryId, VendorPlayerAuthentication vpa, String rawRequest)
    {
        Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

        BetHistorySeamlessRequest dataSet = new BetHistorySeamlessRequest(betHistoryId+"_1", "betResult", "SLOT",
                "PP", vpa.getVendorCurrencyCode(), rawRequest, aggregatorRequestStartMs.toString(), betHistoryId);

        Gson gson = new GsonBuilder().create();

        kafkaTemplate.send("topic_seamless_bet_transformation", betHistoryId, gson.toJson(dataSet));
    }

    public VendorPlayerAuthentication verifyToken(String token) throws AuthenticationException {
        VendorPlayerAuthentication authenticatedUser = new VendorPlayerAuthentication();

        authenticatedUser = vendorPlayerAuthenticationRepository.findByTraceId(token);

        Optional.ofNullable(authenticatedUser).orElseThrow(AuthenticationException::new);

        return authenticatedUser;
    }

    public String verifyCredential(Long vendorCredentialId) throws UnableToFindCredentialsException {
        Map<String, String> credentialMap = new HashMap<String, String>();
        List<VendorCredentialValueReader> vendorCredentialValueReader;
        VendorCredentialReader vendorCredentialReader;

        vendorCredentialReader = vendorCredentialReaderManager.findById(vendorCredentialId).orElse(null);

        //check credentials validity and latest version
        Optional.ofNullable(vendorCredentialReader).orElseThrow(UnableToFindCredentialsException::new);

        vendorCredentialValueReader = vendorCredentialValueReaderManager.findByVendorCredentialIdAndVersion(
                vendorCredentialId, vendorCredentialReader.getLatestVersion());

        //check credentials value validity
        Optional.ofNullable(vendorCredentialValueReader).orElseThrow(UnableToFindCredentialsException::new);

        VendorCredentialValueReader credentialValues = vendorCredentialValueReader.stream()
                .filter(key -> key.getKey().equals("secretKey"))
                .findFirst()
                .orElse(null);

        return credentialValues.getValue();
    }

    public BigDecimal getBetRequestBalanceFromGRPC(ResultDto dto, String traceId, VendorPlayerAuthentication vpa,
                                                   SeamlessBetHistoryRequest seamlessBetRequest) {
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
