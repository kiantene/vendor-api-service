package com.nextgen.gameaggregator.vendor.api.pragmaticplay.service;


import com.google.gson.Gson;
import com.nextgen.gameaggregator.grpc.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcVo;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.service.vo.SeamlessGameLoginVo;
import com.nextgen.gameaggregator.vendor.component.constant.Constant;
import com.nextgen.gameaggregator.vendor.component.vendor.AbstractVendor;
import com.nextgen.gameaggregator.vendor.component.vendor.InterfaceSeamlessVendor;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessBetHistoryOthersRequest;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessBetHistoryRequest;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessBetHistoryResult;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.VendorPlayerAuthentication;
import com.nextgen.gameaggregator.vendor.exception.VendorApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.SEAMLESS_GAME_LOGIN;
import static com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.VENDOR_CODE;

@Component("seamless_pragmaticplayv3.180")
@Scope("prototype")
public class SeamlessPragmaticPlayV3_180 extends AbstractVendor implements InterfaceSeamlessVendor {

//    resultType =
//    1 : win
//    2 : jackpot
//    3 : free game
//    4 : game ended / lose (to be confirmed)
//    5 : promo win

    private static final Logger logger = LoggerFactory.getLogger(SeamlessPragmaticPlayV3_180.class);
    public SeamlessPragmaticPlayV3_180() {
    }

    public SeamlessPragmaticPlayV3_180(Long vendorId, Long vendorCredentialId) {
        this.setVendorIdAndCredentialId(vendorId, vendorCredentialId);
    }

    SeamlessBetHistoryRequest seamlessBetHistoryRequest;
    SeamlessBetHistoryResult seamlessBetHistoryResult;
    SeamlessBetHistoryOthersRequest seamlessBetHistoryOthersRequest;
    VendorPlayerAuthentication vendorPlayerAuthentication;

    //region Game Login
    @Override
    public GameLoginGrpcVo gameLogin(GameLoginGrpcDto dto) {
        try {
            this.setCredential();
            this.findVendorPlayerUsername(dto.getAgentPlayerId(), dto.getAgentId(),
                    dto.getMasterAgentId(), dto.getHouseId(), dto.getCurrency(),true);

            this.createPlayerAuthentication(
                    dto.getWalletType(), dto.getAgentPlayerId(),
                    this.vendorPlayerReader.getId(), this.vendorPlayerReader.getVendorUsername(),dto.getPlatform(),
                    this.findVendorPlatformCode(dto.getPlatform()), dto.getLanguage(),
                    this.findVendorLanguageCode(dto.getLanguage()), dto.getGameId(),
                    this.findVendorGameCode(dto.getGameId(), dto.getLanguage(), dto.getPlatform()), dto.getAgentId(),
                    dto.getTraceId(), dto.getCurrency(), this.findVendorCurrencyCode(dto.getCurrency(), dto.getVendorId()),
                    Instant.now().toEpochMilli(), dto.getVendorCredentialId());


            MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
            paramMap.add("symbol", this.findVendorGameCode(dto.getGameId(), dto.getLanguage(), dto.getPlatform()));
            paramMap.add("language", this.findVendorLanguageCode(dto.getLanguage()));
            paramMap.add("currency", this.findVendorCurrencyCode(dto.getCurrency(), dto.getVendorId()));
            paramMap.add("platform", this.findVendorPlatformCode(dto.getPlatform()));
            paramMap.add("secureLogin", credentialMap.get("secureLogin"));
            paramMap.add("token", dto.getTraceId());
            String response = this.vendorAPICall(paramMap, SEAMLESS_GAME_LOGIN);
            return this.verifyGameLoginResponse(response);
        }catch (VendorApiException vendorApiException) {
            return GameLoginGrpcVo.newBuilder()
                    .setStatus(false)
                    .setGameUrl("")
                    .setVendorErrorCode(vendorApiException.getErrorCode())
                    .setVendorErrorMessage( ConstantErrorMessage.EXTERNAL + "-" +vendorApiException.getMessage())
                    .build();
        } catch (Exception exception) {
            logger.error(exception.getMessage());
            return GameLoginGrpcVo.newBuilder()
                    .setStatus(false)
                    .setGameUrl("")
                    .setVendorErrorCode(ConstantErrorMessage.UNEXPECTED_ERROR_CODE)
                    .setVendorErrorMessage( ConstantErrorMessage.UNEXPECTED_ERROR)
                    .build();
        }


    }

    @Override
    public GameLoginGrpcVo verifyGameLoginResponse(String response) throws VendorApiException {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        SeamlessGameLoginVo seamlessGameLoginVo = new Gson().fromJson(response, SeamlessGameLoginVo.class);
        if(seamlessGameLoginVo ==null){
            throw new VendorApiException(
                    ConstantErrorMessage.INVALID_RESPONSE_CODE,
                    ConstantErrorMessage.EXTERNAL + "-" + ConstantErrorMessage.INVALID_RESPONSE_CODE);
        }

        Set<ConstraintViolation<SeamlessGameLoginVo>> violations = validator.validate(seamlessGameLoginVo);

        if (violations.size() > 0) {
            //TODO log validation Message with traceId
            String validationMessage = "";
            for (ConstraintViolation<SeamlessGameLoginVo> violation : violations) {
                validationMessage += "," + violation.getPropertyPath() + violation.getMessage();
            }
            logger.error(validationMessage);
            throw new VendorApiException(
                    ConstantErrorMessage.INVALID_RESPONSE_CODE,
                    ConstantErrorMessage.EXTERNAL + "-" + ConstantErrorMessage.INVALID_RESPONSE);
        }

        if(seamlessGameLoginVo.getError().equals("0")){
            return GameLoginGrpcVo.newBuilder()
                    .setStatus(true)
                    .setGameUrl(seamlessGameLoginVo.getGameURL())
                    .setVendorErrorCode(ConstantErrorMessage.SUCCESS_CODE)
                    .setVendorErrorMessage(ConstantErrorMessage.SUCCESS_MESSAGE)
                    .build();
        }else{
            return GameLoginGrpcVo.newBuilder()
                    .setStatus(false)
                    .setGameUrl(seamlessGameLoginVo.getGameURL())
                    .setVendorErrorCode(ConstantErrorMessage.EXTERNAL_FAIL_CODE)
                    .setVendorErrorMessage( ConstantErrorMessage.EXTERNAL + "-" +seamlessGameLoginVo.getDescription())
                    .build();

        }

    }

    //endregion


    //region vendor api call
    public String vendorAPICall(MultiValueMap<String, String> paramMap, String endPoint) throws VendorApiException {

        try {
        paramMap.add("hash", this.mD5(URLDecoder.decode(kSort(paramMap)) + this.credentialMap.get("secretKey")));
        WebClient webClient = WebClient.create(this.credentialMap.get("apiUrl"));

        return webClient.post()
                .uri(endPoint)
                // .header("Authorization", "Bearer MY_SECRET_TOKEN")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.ALL)
                .body(BodyInserters.fromFormData(paramMap))
                //.bodyValue(requestJson)
                //.body(BodyInserters.fromValue(paramMap))
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(String.class)
                                .doOnNext(responseBody ->
                                        logger.error("Error Vendor API response from server: {}", responseBody)
                                )
                                // throw original error
                                .then(response.createException())
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(Constant.SERVICE_TIMEOUT))
                .block();
        } catch (Exception exception) {
            throw new VendorApiException(
                    ConstantErrorMessage.INVALID_RESPONSE_CODE,
                    ConstantErrorMessage.EXTERNAL + "-" + exception.getMessage());
        }
    }

    @Override
    public HashMap<String, Object> gameAuthentication(HashMap<String, Object> map) {
        return null;
    }

    @Override
    public HashMap<String, Object> walletBalance(HashMap<String, Object> map) {
        return null;
    }

    @Override
    public HashMap<String, Object> betResult(HashMap<String, Object> map) {

        SeamlessBetHistoryRequest seamlessBetHistoryRequest = new SeamlessBetHistoryRequest();
        SeamlessBetHistoryOthersRequest seamlessBetHistoryOthersRequest = new SeamlessBetHistoryOthersRequest();

        HashMap<String, Object> results = new HashMap<String, Object>();
        String betHistoryId = null;
        VendorPlayerAuthentication v1 = (VendorPlayerAuthentication) map.get("vendorPlayerAuthentication");

        //default error
        results.put("error", 100);

        try{
            seamlessBetHistoryRequest = this.findIdWithVendorBetIdFromLogSeamlessBetHistoryRequest(VENDOR_CODE+"_"+map.get("roundId").toString());
            Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

            if(seamlessBetHistoryRequest == null){
                //if request of this bet is not exists in request table
                //insert data into raw.bet_history_seamless_others table for (ksql?) error record processing
                //will still send as correct result to operator via grpc
                betHistoryId = UUID.randomUUID().toString();

                //map.get("roundId").toString()+"_1"
                this.createRawBetHistorySeamlessOthersCouchBase(betHistoryId+"_1", "betResult",
                    "", VENDOR_CODE, v1.getVendorCurrencyCode(),
                    map.get("rawRequest").toString(), aggregatorRequestStartMs.toString(), betHistoryId);

                //region return success responses
                results.put("error", 0);
                results.put("betHistoryId", betHistoryId);
                results.put("betTime", map.get("timestamp").toString());
                results.put("betAmount", "0");
                results.put("winLoss", map.get("amount").toString());
                results.put("resultType", "1");
                //end region
            }else{
                betHistoryId = seamlessBetHistoryRequest.getBetHistoryId();
                //correct scenario 2, request got matched vendor bet id and result got no matched vendor bet id
                //we proceed to create the result data into log.seamless_bet_history_result table and raw.bet_history_seamless_result table
                //will send as correct result to operator via grpc
                betHistoryId = this.createLogSeamlessBetHistoryResultCouchBase(VENDOR_CODE+"_"+map.get("roundId").toString()+"_1",
                    map.get("reference").toString(), betHistoryId, map.get("roundId").toString(),
                    seamlessBetHistoryRequest.getBetAmount(), Double.parseDouble(map.get("amount").toString()),
                    seamlessBetHistoryRequest.getBetTime(), Long.parseLong(map.get("timestamp").toString()),
                    Instant.now().toEpochMilli(), "betResult", "SLOT", map.get("rawRequest").toString(),
                    VENDOR_CODE);

                //the data insert to bet_history_seamless_result table, have to determine the result type in betHistoryId
                this.createRawBetHistorySeamlessResultCouchBase(betHistoryId+"_1", "betResult",
                    "", VENDOR_CODE, v1.getVendorCurrencyCode(),
                    map.get("rawRequest").toString(), aggregatorRequestStartMs.toString(), betHistoryId);


                //region return success responses
                results.put("error", 0);
                results.put("description", "Success");
                results.put("betHistoryId", betHistoryId);
                results.put("betTime", seamlessBetHistoryRequest.getBetTime().toString());
                results.put("betAmount", seamlessBetHistoryRequest.getBetAmount().toString());
                results.put("winLoss", map.get("amount").toString());
                results.put("resultType", "1");
                //end region

            }

        } catch (Exception e){
            //if any of the db insert is failed, will throw error
            logger.error("betResult insert error : " + e);
            logger.error("seamlessBetHistoryRequest : " + seamlessBetHistoryRequest);
        }

        return results;
    }

    @Override
    public HashMap<String, Object> betRequest(HashMap<String, Object> map) {

        SeamlessBetHistoryRequest seamlessBetHistoryRequest = new SeamlessBetHistoryRequest();
        HashMap<String, Object> results = new HashMap<String, Object>();
        String betHistoryId = null;
        VendorPlayerAuthentication v1 = (VendorPlayerAuthentication) map.get("vendorPlayerAuthentication");

        //default error
        results.put("error", 100);

        try{
            seamlessBetHistoryRequest = this.findIdWithVendorBetIdFromLogSeamlessBetHistoryRequest(VENDOR_CODE+"_"+map.get("roundId").toString());

            if(seamlessBetHistoryRequest == null){
                //proceed to create the record in log.seamless_bet_history_collections table, this is used for check is request exists
                //will still send as correct result to operator via grpc
                betHistoryId = this.createLogSeamlessBetHistoryRequestCouchBase(VENDOR_CODE+"_"+map.get("roundId").toString(),
                        map.get("reference").toString(), UUID.randomUUID().toString(), map.get("roundId").toString(),
                        Double.parseDouble(map.get("amount").toString()), Long.parseLong(map.get("timestamp").toString()),
                        Instant.now().toEpochMilli(), "betRequest", "SLOT", map.get("rawRequest").toString(),
                        VENDOR_CODE);

                Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

                //proceed to create record to raw.bet_history_seamless_request table, this is used for ksql aggregate data
                this.createRawBetHistorySeamlessRequestCouchBase(betHistoryId, "betRequest",
                        "", VENDOR_CODE, v1.getVendorCurrencyCode(),
                        map.get("rawRequest").toString(), aggregatorRequestStartMs.toString());

            }else{
                //will still send as correct result to operator via grpc
                betHistoryId = seamlessBetHistoryRequest.getBetHistoryId();
            }
            //region return success responses
            results.put("error", 0);
            results.put("betHistoryId", betHistoryId);
            //end region

        } catch (Exception e){
            //if any of the db insert is failed, will throw error
            logger.error("betRequest insert error : " + e);
            logger.error("seamlessBetHistoryRequest: " + seamlessBetHistoryRequest);
        }

        return results;
    }

    @Override
    public HashMap<String, Object> endRound(HashMap<String, Object> map) {

        SeamlessBetHistoryRequest seamlessBetHistoryRequest = new SeamlessBetHistoryRequest();
        SeamlessBetHistoryOthersRequest seamlessBetHistoryOthersRequest = new SeamlessBetHistoryOthersRequest();

        HashMap<String, Object> results = new HashMap<String, Object>();
        String betHistoryId = null;
        VendorPlayerAuthentication v1 = (VendorPlayerAuthentication) map.get("vendorPlayerAuthentication");

        //default error
        results.put("error", 100);

        try{
            seamlessBetHistoryRequest = this.findIdWithVendorBetIdFromLogSeamlessBetHistoryRequest(VENDOR_CODE+"_"+map.get("roundId").toString());
            Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

            if(seamlessBetHistoryRequest == null){
                //if request of this bet is not exists in request table
                //insert data into raw.bet_history_seamless_others table for (ksql?) error record processing
                //will still send as correct result to operator via grpc
                betHistoryId = UUID.randomUUID().toString();

                //map.get("roundId").toString()+"_4"
                this.createRawBetHistorySeamlessOthersCouchBase(betHistoryId+"_4", "endRound",
                        "", VENDOR_CODE, v1.getVendorCurrencyCode(),
                        map.get("rawRequest").toString(), aggregatorRequestStartMs.toString(), betHistoryId);

                //region return success responses
                results.put("error", 0);
                //end region
            }else{
                betHistoryId = seamlessBetHistoryRequest.getBetHistoryId();
                //correct scenario 2, request got matched vendor bet id and result got no matched vendor bet id
                //we proceed to create the result data into log.seamless_bet_history_result table and raw.bet_history_seamless_result table
                //will send as correct result to operator via grpc
                betHistoryId = this.createLogSeamlessBetHistoryResultCouchBase(VENDOR_CODE+"_"+map.get("roundId").toString()+"_4",
                        seamlessBetHistoryRequest.getVendorBetId(), betHistoryId, map.get("roundId").toString(),
                        seamlessBetHistoryRequest.getBetAmount(), 0d,
                        seamlessBetHistoryRequest.getBetTime(), seamlessBetHistoryRequest.getBetTime(),
                        Instant.now().toEpochMilli(), "endRound", "SLOT", map.get("rawRequest").toString(),
                        VENDOR_CODE);

                //the data insert to bet_history_seamless_result table, have to determine the result type in betHistoryId
                this.createRawBetHistorySeamlessResultCouchBase(betHistoryId+"_4", "endRound",
                        "", VENDOR_CODE, v1.getVendorCurrencyCode(),
                        map.get("rawRequest").toString(), aggregatorRequestStartMs.toString(), betHistoryId);

                //region return success responses
                results.put("error", 0);
                //end region
            }

        } catch (Exception e){
            //if any of the db insert is failed, will throw error
            logger.error("endRound insert error : " + e);
            logger.error("seamlessBetHistoryRequest : " + seamlessBetHistoryRequest);
        }

        return results;
    }

    @Override
    public HashMap<String, Object> refund(HashMap<String, Object> map) {
        SeamlessBetHistoryRequest seamlessBetHistoryRequest = new SeamlessBetHistoryRequest();
        SeamlessBetHistoryResult seamlessBetHistoryResult = new SeamlessBetHistoryResult();

        HashMap<String, Object> results = new HashMap<String, Object>();
        VendorPlayerAuthentication v1 = (VendorPlayerAuthentication) map.get("vendorPlayerAuthentication");

        //default error
        results.put("error", 100);

        try{
            seamlessBetHistoryRequest = this.findVendorBetIdFromSeamlessBetHistoryRequest(map.get("reference").toString());
            seamlessBetHistoryResult = this.findVendorBetIdFromSeamlessBetHistoryResult(map.get("reference").toString());

            if(seamlessBetHistoryResult != null){
                String resultType = seamlessBetHistoryResult.getBetHistoryId().substring(seamlessBetHistoryResult.
                        getBetHistoryId().length() - 1);

                results.put("vendorRoundId", seamlessBetHistoryResult.getVendorRoundId());
                results.put("betAmount", seamlessBetHistoryResult.getBetAmount());
                results.put("refundAmount", seamlessBetHistoryResult.getWinLoss());
                results.put("resultType", resultType);
                results.put("betTime", seamlessBetHistoryResult.getBetTime());
                results.put("settledTime", seamlessBetHistoryResult.getSettledTime());
                results.put("betHistoryId", seamlessBetHistoryResult.getBetHistoryId());
                results.put("error", 0);
            }
            else if(seamlessBetHistoryRequest != null){
                results.put("vendorRoundId", seamlessBetHistoryRequest.getVendorRoundId());
                results.put("betAmount", seamlessBetHistoryRequest.getBetAmount());
                results.put("refundAmount", seamlessBetHistoryRequest.getBetAmount());
                results.put("resultType", "0");
                results.put("betTime", seamlessBetHistoryRequest.getBetTime());
                results.put("settledTime", seamlessBetHistoryRequest.getBetTime());
                results.put("betHistoryId", seamlessBetHistoryRequest.getBetHistoryId());
                results.put("error", 0);
            }
            else{
                //TODO THIS WILL KEEP TRIGGER FOR US, NEED TO HANDLE ?
                logger.error("refund record for reference_id "+ map.get("reference").toString() +" unable to find in request and result table couchbase");
            }

        } catch (Exception e){
            logger.error("refund insert error : " + e);
            logger.error("seamlessBetHistoryRequest : " + seamlessBetHistoryRequest);
            logger.error("seamlessBetHistoryResult : " + seamlessBetHistoryResult);
        }

        return results;
    }

    @Override
    public HashMap<String, Object> bonusWin(HashMap<String, Object> map) {
        SeamlessBetHistoryRequest seamlessBetHistoryRequest = new SeamlessBetHistoryRequest();
        SeamlessBetHistoryOthersRequest seamlessBetHistoryOthersRequest = new SeamlessBetHistoryOthersRequest();

        HashMap<String, Object> results = new HashMap<String, Object>();
        String betHistoryId = null;
        VendorPlayerAuthentication v1 = (VendorPlayerAuthentication) map.get("vendorPlayerAuthentication");

        //default error
        results.put("error", 100);

        try{
            seamlessBetHistoryRequest = this.findIdWithVendorBetIdFromLogSeamlessBetHistoryRequest(VENDOR_CODE+"_"+map.get("roundId").toString());
            Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

            if(seamlessBetHistoryRequest == null){
                //if request of this bet is not exists in request table
                //insert data into raw.bet_history_seamless_others table for (ksql?) error record processing
                //will still send as correct result to operator via grpc
                betHistoryId = UUID.randomUUID().toString();

                //map.get("roundId").toString()+"_3"
                this.createRawBetHistorySeamlessOthersCouchBase(betHistoryId+"_3", "bonusWin",
                        "", VENDOR_CODE, v1.getVendorCurrencyCode(),
                        map.get("rawRequest").toString(), aggregatorRequestStartMs.toString(), betHistoryId);

                //region return success responses
                results.put("error", 0);
                //end region
            }else{
                betHistoryId = seamlessBetHistoryRequest.getBetHistoryId();
                //correct scenario 2, request got matched vendor bet id and result got no matched vendor bet id
                //we proceed to create the result data into log.seamless_bet_history_result table and raw.bet_history_seamless_result table
                //will send as correct result to operator via grpc
                betHistoryId = this.createLogSeamlessBetHistoryResultCouchBase(VENDOR_CODE+"_"+map.get("roundId").toString()+"_3",
                        seamlessBetHistoryRequest.getVendorBetId(), betHistoryId, map.get("roundId").toString(),
                        seamlessBetHistoryRequest.getBetAmount(), Double.parseDouble(map.get("amount").toString()),
                        seamlessBetHistoryRequest.getBetTime(), Long.parseLong(map.get("timestamp").toString()),
                        Instant.now().toEpochMilli(), "bonusWin", "SLOT", map.get("rawRequest").toString(),
                        VENDOR_CODE);

                //the data insert to bet_history_seamless_result table, have to determine the result type in betHistoryId
                this.createRawBetHistorySeamlessResultCouchBase(betHistoryId+"_3", "bonusWin",
                        "", VENDOR_CODE, v1.getVendorCurrencyCode(),
                        map.get("rawRequest").toString(), aggregatorRequestStartMs.toString(), betHistoryId);

                //region return success responses
                results.put("error", 0);
                //end region
            }

        } catch (Exception e){
            //if any of the db insert is failed, will throw error
            logger.error("bonusWin insert error : " + e);
            logger.error("seamlessBetHistoryRequest : " + seamlessBetHistoryRequest);
        }

        return results;
    }

    @Override
    public HashMap<String, Object> jackpotWin(HashMap<String, Object> map) {
        SeamlessBetHistoryRequest seamlessBetHistoryRequest = new SeamlessBetHistoryRequest();
        SeamlessBetHistoryOthersRequest seamlessBetHistoryOthersRequest = new SeamlessBetHistoryOthersRequest();

        HashMap<String, Object> results = new HashMap<String, Object>();
        String betHistoryId = null;
        VendorPlayerAuthentication v1 = (VendorPlayerAuthentication) map.get("vendorPlayerAuthentication");

        //default error
        results.put("error", 100);

        try{
            seamlessBetHistoryRequest = this.findIdWithVendorBetIdFromLogSeamlessBetHistoryRequest(VENDOR_CODE+"_"+map.get("roundId").toString());
            Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

            if(seamlessBetHistoryRequest == null){
                //if request of this bet is not exists in request table
                //insert data into raw.bet_history_seamless_others table for (ksql?) error record processing
                //will still send as correct result to operator via grpc
                betHistoryId = UUID.randomUUID().toString();

                //map.get("roundId").toString()+"_2"
                this.createRawBetHistorySeamlessOthersCouchBase(betHistoryId+"_2", "jackpotWin",
                        "", VENDOR_CODE, v1.getVendorCurrencyCode(),
                        map.get("rawRequest").toString(), aggregatorRequestStartMs.toString(), betHistoryId);

                //region return success responses
                results.put("error", 0);
                results.put("description", "Success");
                results.put("betHistoryId", betHistoryId);
                results.put("betTime", map.get("timestamp").toString());
                results.put("betAmount", "0");
                //end region
            }else{
                betHistoryId = seamlessBetHistoryRequest.getBetHistoryId();
                //correct scenario 2, request got matched vendor bet id and result got no matched vendor bet id
                //we proceed to create the result data into log.seamless_bet_history_result table and raw.bet_history_seamless_result table
                //will send as correct result to operator via grpc
                betHistoryId = this.createLogSeamlessBetHistoryResultCouchBase(VENDOR_CODE+"_"+map.get("roundId").toString()+"_2",
                        map.get("reference").toString(), betHistoryId, map.get("roundId").toString(),
                        seamlessBetHistoryRequest.getBetAmount(), Double.parseDouble(map.get("amount").toString()),
                        seamlessBetHistoryRequest.getBetTime(), Long.parseLong(map.get("timestamp").toString()),
                        Instant.now().toEpochMilli(), "jackpotWin", "SLOT", map.get("rawRequest").toString(),
                        VENDOR_CODE);

                //the data insert to bet_history_seamless_result table, have to determine the result type in betHistoryId
                this.createRawBetHistorySeamlessResultCouchBase(betHistoryId+"_2", "jackpotWin",
                        "", VENDOR_CODE, v1.getVendorCurrencyCode(),
                        map.get("rawRequest").toString(), aggregatorRequestStartMs.toString(), betHistoryId);


                //region return success responses
                results.put("error", 0);
                results.put("description", "Success");
                results.put("betHistoryId", betHistoryId);
                results.put("betTime", seamlessBetHistoryRequest.getBetTime().toString());
                results.put("betAmount", seamlessBetHistoryRequest.getBetAmount().toString());
                //end region

            }
            results.put("jackpotWin", map.get("amount").toString());
            results.put("resultType", "2");

        } catch (Exception e){
            //if any of the db insert is failed, will throw error
            logger.error("jackpotWin insert error : " + e);
            logger.error("seamlessBetHistoryRequest : " + seamlessBetHistoryRequest);
        }

        return results;
    }


    // encryption md5 method
    private String mD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            //algorithm exception...
        }
        return null;
    }
    // k-sort data, rearrange the request params from A-Z
    private String kSort(MultiValueMap<String, String> map) {

        String sb = "";
        String[] key = new String[map.size()];
        int index = 0;
        for (String k : map.keySet()) {
            key[index] = k;
            index++;
        }
        Arrays.sort(key);
        for (String s : key) {
            sb += s + "=" + map.getFirst(s) + "&";
        }
        sb = sb.substring(0, sb.length() - 1);
        // 将得到的字符串进行处理得到目标格式的字符串
        try {
            sb = URLEncoder.encode(sb, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }// 使用常见的UTF-8编码
        sb = sb.replace("%3D", "=").replace("%26", "&");

        return sb;
    }
    //endregion
}
