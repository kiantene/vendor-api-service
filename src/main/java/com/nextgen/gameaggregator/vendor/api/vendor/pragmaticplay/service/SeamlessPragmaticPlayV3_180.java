package com.nextgen.gameaggregator.vendor.api.vendor.pragmaticplay.service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nextgen.gameaggregator.vendor.api.vendor.pragmaticplay.service.dto.SeamlessGameLoginResponseDto;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessBetHistoryRequest;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessBetHistoryResult;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorPlayerAuthenticationReader;
import com.nextgen.gameaggregator.vendor.grpc.dto.VendorGameLoginServiceRequestDto;
import com.nextgen.gameaggregator.vendor.grpc.vo.VendorGameLoginServiceResponseVo;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorPlayerReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity.VendorPlayerWriter;
import com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.seamless.AbstractSeamlessVendor;
import com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.seamless.InterfaceSeamlessVendor;
import com.nextgen.gameaggregator.vendor.util.NameUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.nextgen.gameaggregator.vendor.api.vendor.pragmaticplay.constant.RequestConstant.VENDOR_CODE;
import static com.nextgen.gameaggregator.vendor.api.vendor.pragmaticplay.constant.RequestConstant.VENDOR_SEAMLESS_GAME_LOGIN;
import static com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.constant.ConstantErrorMessage.*;

@Component("seamless_pragmaticplayv3.180")
public class SeamlessPragmaticPlayV3_180 extends AbstractSeamlessVendor implements InterfaceSeamlessVendor {
    //TODO ERROR MAPPING
    //TODO COMPONENT WALLET_TYPE MAPPING
    //TODO INTERFACE PARAMS VALIDATION
    //TODO CUSTOM BET_HISTORY_ID
    private Map<String, String> credentialMap = new HashMap<>();
    SeamlessBetHistoryRequest seamlessBetHistoryRequest;
    SeamlessBetHistoryResult seamlessBetHistoryResult;

    //region game login
    @Override
    public VendorGameLoginServiceResponseVo gameLogin(VendorGameLoginServiceRequestDto dto) {

        VendorPlayerReader vendorPlayerReader;
        VendorPlayerWriter vendorPlayerWriter;
        VendorGameLoginServiceResponseVo vendorGameLoginServiceResponseVo;

        Long latestVersion;
        String vendorPlayerUsername = null;
        String vendorPlatformCode = this.findVendorPlatformCode(dto.getVendorId(), dto.getPlatform());
        String vendorLanguageCode = this.findVendorLanguageCode(dto.getVendorId(), dto.getLanguage());
        String vendorGameCode = this.findVendorGameCode(dto.getGameId(), dto.getLanguage(), dto.getPlatform());
        String vendorCurrencyCode = this.findVendorCurrencyCode(dto.getCurrency(), dto.getVendorId());
        Long vendorPlayerId;

        //find the latest version of the vendor from vendor_credentials table
        latestVersion = this.findVendorLatestCredentialVersion(dto.getVendorCredentialId(), dto.getVendorId(),
                dto.getHouseId());

        //find credentials key and value from vendor_credential_values table
        credentialMap = this.findVendorCredentialKeyAndValue(dto.getVendorCredentialId(), latestVersion);

        //find vendor player username from vendor_players table
        vendorPlayerReader = this.findVendorPlayerUsername(dto.getAgentPlayerId(), dto.getVendorId(),
                dto.getVendorCredentialId(), dto.getCurrency());

        //region if vendor player does not exist for this player
        if (vendorPlayerReader == null) {
           //generate vendor username based on vendor requirements
            vendorPlayerUsername = NameUtils.generateUsername("0", dto.getVendorCredentialId(), dto.getAgentId(), dto.getAgentPlayerId());

            //create vendor username and get the id from vendor_players table
            vendorPlayerWriter = this.createAndGetVendorPlayerUsername(dto.getAgentPlayerId(), dto.getVendorId(),
                    dto.getVendorCredentialId(), latestVersion, vendorPlayerUsername, dto.getCurrency());

            vendorPlayerId = vendorPlayerWriter.getId();
        } else {
            vendorPlayerUsername = vendorPlayerReader.getVendorUsername();
            vendorPlayerId = vendorPlayerReader.getId();
        }
        //endregion

        //create player's session into vendor_player_authentication table
        this.createAndGetTraceId(dto.getVendorId(), dto.getWalletType(), dto.getAgentPlayerId(), vendorPlayerId,
                vendorPlayerUsername, dto.getPlatform(), vendorPlatformCode,
                dto.getLanguage(), vendorLanguageCode, dto.getGameId(), vendorGameCode, dto.getAgentId(),
                dto.getTraceId(), dto.getCurrency(), vendorCurrencyCode);

        //mapping data into vendor's needed format to perform api call
        Map<String, String> map = new HashMap<>();
        map.put("secureLogin", credentialMap.get("secureLogin"));
        map.put("symbol", vendorGameCode);
        map.put("language", vendorLanguageCode);
        map.put("token", dto.getTraceId());
        map.put("currency", vendorCurrencyCode);
        map.put("platform", vendorPlatformCode);

        //prepare to call API
        ResponseEntity<String> response = this.vendorAPICall(map, VENDOR_SEAMLESS_GAME_LOGIN);

        //check vendor response
        vendorGameLoginServiceResponseVo = this.verifyGameLoginApiResponse(response);

        return vendorGameLoginServiceResponseVo;
    }
    //endregion

    //region check authentication of player
    @Override
    public HashMap<String, Object> gameAuthentication(HashMap<String, Object> map) {

        HashMap<String, Object> results = new HashMap<String, Object>();
        VendorPlayerAuthenticationReader vendorPlayerAuthenticationReader;

        //region default error
        results.put("userId", "");
        results.put("currency", "");
        results.put("cash", 0d);
        results.put("bonus", 0d);
        results.put("token", map.get("token").toString());
        results.put("error", 4);
        results.put("description", "Player authentication failed due to invalid, not found or expired token.");
        //endregion

        //do we need to compare the hash detail that is stored in vendor_player_authentications table?
        vendorPlayerAuthenticationReader = this.findTraceId(map.get("token").toString());

        if (vendorPlayerAuthenticationReader == null){
            //to be decided, because it should be same as default error defined above...
        } else {
            //the player token can be found in vendor_player_authentications table
            results.put("userId", vendorPlayerAuthenticationReader.getVendorPlayerUsername());
            results.put("currency", vendorPlayerAuthenticationReader.getVendorCurrencyCode());
            results.put("cash", 0d);
            results.put("bonus", 0d);
            results.put("token", vendorPlayerAuthenticationReader.getTraceId());
            results.put("error", 0);
            results.put("description", "Success");
            results.put("vendorPlayerAuthenticationReader", vendorPlayerAuthenticationReader);
        }

        return results;
    }
    //endregion

    @Override
    public String gameLogout() {
        return null;
    }

    //region check wallet balance of player
    @Override
    public HashMap<String, Object> walletBalance(HashMap<String, Object> map) {

        HashMap<String, Object> results = new HashMap<String, Object>();
        VendorPlayerAuthenticationReader vendorPlayerAuthenticationReader;

        //region default error
        results.put("currency", "");
        results.put("cash", 0d);
        results.put("bonus", 0d);
        results.put("error", 4);
        results.put("description", "Player authentication failed due to invalid, not found or expired token.");
        //endregion

        //do we need to compare the hash detail that is stored in vendor_player_authentications table?
        vendorPlayerAuthenticationReader = this.findTraceId(map.get("token").toString());

        if (vendorPlayerAuthenticationReader == null){
            //to be decided, because it should be same as default error defined above...
        } else {
            //the player token can be found in vendor_player_authentications table
            results.put("currency", vendorPlayerAuthenticationReader.getVendorCurrencyCode());
            results.put("cash", 0d);
            results.put("bonus", 0d);
            results.put("error", 0);
            results.put("description", "Success");
            results.put("vendorPlayerAuthenticationReader", vendorPlayerAuthenticationReader);
        }

        return results;
    }
    //endregion

    //region process bet request
    @Override
    public HashMap<String, Object> betRequest(HashMap<String, Object> map) {

        HashMap<String, Object> results = new HashMap<String, Object>();
        VendorPlayerAuthenticationReader vendorPlayerAuthenticationReader;
        String betHistoryId = null;

        //region default error
        results.put("transactionId", "");
        results.put("currency", "");
        results.put("cash", 0d);
        results.put("bonus", 0d);
        results.put("usedPromo", 0d);
        results.put("error", 4);
        results.put("description", "Player authentication failed due to invalid, not found or expired token.");
        //endregion

        //do we need to compare the hash detail that is stored in vendor_player_authentications table?
        vendorPlayerAuthenticationReader = this.findTraceId(map.get("token").toString());

        if (vendorPlayerAuthenticationReader == null){
            //to be decided, because it should be same as default error defined above...
        } else {
            //the player token can be found in vendor_player_authentications table
            //check is betHistoryId is inside seamless_bet_history_collections table
            seamlessBetHistoryRequest = this.findServiceCodeWithVendorBetIdFromLogSeamlessBetHistoryRequest(VENDOR_CODE+"_"+map.get("roundId").toString());

            if(seamlessBetHistoryRequest == null){
                //proceed to create the record in seamless_bet_history_collections table
                //TODO GET GAME CATEGORY

                betHistoryId = this.createLogSeamlessBetHistoryRequestCouchBase(VENDOR_CODE+"_"+map.get("roundId").toString(),
                        map.get("reference").toString(), UUID.randomUUID().toString(), map.get("roundId").toString(),
                        Double.parseDouble(map.get("amount").toString()), Long.parseLong(map.get("timestamp").toString()),
                        Instant.now().toEpochMilli(), "betRequest", "SLOT", map.get("rawRequest").toString(),
                        VENDOR_CODE);

                Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

                this.createRawBetHistorySeamlessRequestCouchBase(betHistoryId, "betRequest",
                        "", VENDOR_CODE, vendorPlayerAuthenticationReader.getVendorCurrencyCode(),
                        map.get("rawRequest").toString(), aggregatorRequestStartMs.toString());

            }else{
                betHistoryId = seamlessBetHistoryRequest.getBetHistoryId();
            }

            //region return success responses
            results.put("transactionId", "");
            results.put("currency", vendorPlayerAuthenticationReader.getVendorCurrencyCode());
            results.put("cash", 0d);
            results.put("bonus", 0d);
            results.put("usedPromo", 0d);
            results.put("error", 0);
            results.put("description", "Success");
            results.put("betHistoryId", betHistoryId);
            results.put("betTime", map.get("timestamp").toString());
            results.put("vendorBetId", map.get("reference").toString());
            results.put("vendorRoundId", map.get("roundId").toString());
            results.put("betAmount", map.get("amount").toString());
            results.put("vendorPlayerAuthenticationReader", vendorPlayerAuthenticationReader);
            //end region
        }

        return results;
    }
    //endregion

    //region process bet results
    @Override
    public HashMap<String, Object> betResult(HashMap<String, Object> map) {

        HashMap<String, Object> results = new HashMap<String, Object>();
        VendorPlayerAuthenticationReader vendorPlayerAuthenticationReader;
        String betHistoryId = null;
        Integer correctScenario = 0;

        //region default error
        results.put("transactionId", "");
        results.put("currency", "");
        results.put("cash", 0d);
        results.put("bonus", 0d);
        results.put("error", 4);
        results.put("description", "Player authentication failed due to invalid, not found or expired token.");
        //endregion

        //do we need to compare the hash detail that is stored in vendor_player_authentications table?
        vendorPlayerAuthenticationReader = this.findTraceId(map.get("token").toString());

        if (vendorPlayerAuthenticationReader == null){
            //to be decided, because it should be same as default error defined above...
        } else {
            //the player token can be found in vendor_player_authentications table
            //check is betHistoryId is inside seamless_bet_history_collections table
            seamlessBetHistoryRequest = this.findServiceCodeWithVendorBetIdFromLogSeamlessBetHistoryRequest(VENDOR_CODE+"_"+map.get("roundId").toString());
            seamlessBetHistoryResult = this.findServiceCodeWithVendorBetIdFromLogSeamlessBetHistoryResult(VENDOR_CODE+"_"+map.get("roundId").toString());

            if(seamlessBetHistoryRequest == null){
                if(seamlessBetHistoryResult == null){
                    //correct scenario 1, request got no matched vendor bet id and result got no matched vendor bet id
                    //we proceed to create the result data with bet_amount = 0
                    //betTime and settledTime will be the same
                    betHistoryId = this.createLogSeamlessBetHistoryResultCouchBase(VENDOR_CODE+"_"+map.get("roundId").toString(),
                            map.get("reference").toString(), UUID.randomUUID().toString(), map.get("roundId").toString(),
                            0d, Double.parseDouble(map.get("amount").toString()),
                            Long.parseLong(map.get("timestamp").toString()), Long.parseLong(map.get("timestamp").toString()),
                            Instant.now().toEpochMilli(), "betResult", "SLOT", map.get("rawRequest").toString(),
                            VENDOR_CODE);

                    correctScenario = 1;

                    //region return success responses
                    results.put("transactionId", "");
                    results.put("currency", vendorPlayerAuthenticationReader.getVendorCurrencyCode());
                    results.put("cash", 0d);
                    results.put("bonus", 0d);
                    results.put("error", 0);
                    results.put("description", "Success");
                    results.put("betHistoryId", betHistoryId);
                    results.put("betTime", map.get("timestamp").toString());
                    results.put("settledTime", map.get("timestamp").toString());
                    results.put("vendorBetId", map.get("reference").toString());
                    results.put("vendorRoundId", map.get("roundId").toString());
                    results.put("betAmount", "0");
                    results.put("winLoss", map.get("amount").toString());
                    results.put("resultType", "1");
                    results.put("vendorPlayerAuthenticationReader", vendorPlayerAuthenticationReader);
                    //end region
                }
                else{
                    //error scenario 1, request got no matched vendor bet id and result got matched vendor bet id
                }
            }else{
                betHistoryId = seamlessBetHistoryRequest.getBetHistoryId();
                if(seamlessBetHistoryResult == null) {
                    //correct scenario 2, request got matched vendor bet id and result got no matched vendor bet id
                    //we proceed to create the result data
                    betHistoryId = this.createLogSeamlessBetHistoryResultCouchBase(VENDOR_CODE+"_"+map.get("roundId").toString(),
                            map.get("reference").toString(), betHistoryId, map.get("roundId").toString(),
                            seamlessBetHistoryRequest.getBetAmount(), Double.parseDouble(map.get("amount").toString()),
                            seamlessBetHistoryRequest.getBetTime(), Long.parseLong(map.get("timestamp").toString()),
                            Instant.now().toEpochMilli(), "betResult", "SLOT", map.get("rawRequest").toString(),
                            VENDOR_CODE);

                    correctScenario = 1;

                    //region return success responses
                    results.put("transactionId", "");
                    results.put("currency", vendorPlayerAuthenticationReader.getVendorCurrencyCode());
                    results.put("cash", 0d);
                    results.put("bonus", 0d);
                    results.put("error", 0);
                    results.put("description", "Success");
                    results.put("betHistoryId", betHistoryId);
                    results.put("betTime", seamlessBetHistoryRequest.getBetTime().toString());
                    results.put("settledTime", map.get("timestamp").toString());
                    results.put("vendorBetId", map.get("reference").toString());
                    results.put("vendorRoundId", map.get("roundId").toString());
                    results.put("betAmount", seamlessBetHistoryRequest.getBetAmount().toString());
                    results.put("winLoss", map.get("amount").toString());
                    results.put("resultType", "1");
                    results.put("vendorPlayerAuthenticationReader", vendorPlayerAuthenticationReader);
                    //end region
                }
                else{
                    //error scenario 2, request got matched vendor bet id and result also got matched vendor bet id
                }
            }
        }

        if(correctScenario == 1){
            //when correct scenario, then create data into couchbase for data processing
            Long aggregatorRequestStartMs = Instant.now().toEpochMilli();

            System.out.println("betHistoryId :::" + betHistoryId);
            System.out.println("betResult :::");
            System.out.println("aggregatorRequestStartMs :::" + aggregatorRequestStartMs);

            this.createRawBetHistorySeamlessResultCouchBase(betHistoryId, "betResult",
                    "", VENDOR_CODE, vendorPlayerAuthenticationReader.getVendorCurrencyCode(),
                    map.get("rawRequest").toString(), aggregatorRequestStartMs.toString());
        }

        return results;
    }
    //endregion

    //region verify game login responses from vendor and map to desired output
    private VendorGameLoginServiceResponseVo verifyGameLoginApiResponse(ResponseEntity<String> response){

        SeamlessGameLoginResponseDto seamlessGameLoginResponseDto = null;
        Boolean setStatus;
        VendorGameLoginServiceResponseVo vendorGameLoginServiceResponseVo = new VendorGameLoginServiceResponseVo();

        //region default the data as failed
        vendorGameLoginServiceResponseVo.setStatus(false);
        vendorGameLoginServiceResponseVo.setVendorErrorCode(FAILURE_CODE);
        vendorGameLoginServiceResponseVo.setVendorErrorMessage(FAILURE_MESSAGE);
        vendorGameLoginServiceResponseVo.setGameUrl("");
        //endregion

        try{
            seamlessGameLoginResponseDto = new Gson().fromJson((response.getBody()), SeamlessGameLoginResponseDto.class);
        }catch (JsonParseException e) {
            //check is it json format
            vendorGameLoginServiceResponseVo.setVendorErrorMessage(JSON_ERROR);
        }


        try{
            //check if getError() from vendor is 0 for a success response
            setStatus = seamlessGameLoginResponseDto.getError().equalsIgnoreCase("0");
        }catch (NullPointerException e){
            //check is the getError() value is null
            setStatus = false;
        }


        //to be discussed for the error mapping to our error collection
        this.ErrorMapping();

        //map output back to grpc endpoint
        vendorGameLoginServiceResponseVo.setStatus(setStatus);
        vendorGameLoginServiceResponseVo.setVendorErrorCode(seamlessGameLoginResponseDto.getError());
        vendorGameLoginServiceResponseVo.setVendorErrorMessage(seamlessGameLoginResponseDto.getDescription());
        vendorGameLoginServiceResponseVo.setGameUrl(seamlessGameLoginResponseDto.getGameURL());

        return vendorGameLoginServiceResponseVo;
    }
    //endregion

    //region vendor api call
    private ResponseEntity<String> vendorAPICall(Map<String, String> map, String endPoint){

        map.put("hash", this.mD5(URLDecoder.decode(kSort(map)) + this.credentialMap.get("secretKey")));
        System.out.println("HASH VALUE = "+this.mD5(URLDecoder.decode(kSort(map)) + this.credentialMap.get("secretKey")));
        MultiValueMap<String, Object> body = this.formData(map);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();
        String uri = this.credentialMap.get("apiUrl") + endPoint;

        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);

        return response;
    }
    //endregion

    //vendor encryption md5 method
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
    //endregion

    //form data format as what vendor required
    private MultiValueMap<String, Object> formData(Map<String, String> map) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            body.add(entry.getKey(), entry.getValue());

        }
        System.out.println(body);
        return body;
    }
    //endregion

    //region k-sort data, rearrange the request params from A-Z
    private String kSort(Map<String, String> map) {

        String sb = "";
        String[] key = new String[map.size()];
        int index = 0;
        for (String k : map.keySet()) {
            key[index] = k;
            index++;
        }
        Arrays.sort(key);
        for (String s : key) {
            sb += s + "=" + map.get(s) + "&";
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

    private void ErrorMapping(){
        //error mapping to be discussed...
    }


}
