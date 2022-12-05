package com.nextgen.gameaggregator.vendor.api.pragmaticplay.service;


import com.google.gson.Gson;
import com.nextgen.gameaggregator.grpc.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcVo;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.service.vo.SeamlessGameLoginVo;
import com.nextgen.gameaggregator.vendor.component.constant.Constant;
import com.nextgen.gameaggregator.vendor.component.vendor.AbstractVendor;
import com.nextgen.gameaggregator.vendor.component.vendor.InterfaceSeamlessVendor;
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
import java.util.Arrays;
import java.util.Set;

import static com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.SEAMLESS_GAME_LOGIN;

@Component("seamless_pragmaticplayv3.180")
@Scope("prototype")
public class SeamlessPragmaticPlayV3_180 extends AbstractVendor implements InterfaceSeamlessVendor {

    private static final Logger logger = LoggerFactory.getLogger(SeamlessPragmaticPlayV3_180.class);
    public SeamlessPragmaticPlayV3_180() {
    }

    public SeamlessPragmaticPlayV3_180(Long vendorId, Long vendorCredentialId) {
        this.setVendorIdAndCredentialId(vendorId, vendorCredentialId);
    }

    //region Game Login
    @Override
    public GameLoginGrpcVo gameLogin(GameLoginGrpcDto dto) {
        try {
            this.setCredential();
            this.findVendorPlayerUsername(dto.getAgentPlayerId(), dto.getAgentId(), dto.getCurrency(), true);

            this.createPlayerAuthentication(
                    Long.valueOf(dto.getWalletType()), dto.getAgentPlayerId(),
                    this.vendorPlayerReader.getId(), this.vendorPlayerReader.getVendorUsername(),dto.getPlatform(),
                    this.findVendorPlatformCode(dto.getPlatform()), dto.getLanguage(),
                    this.findVendorLanguageCode(dto.getLanguage()), dto.getGameId(),
                    this.findVendorGameCode(dto.getGameId(), dto.getLanguage(), dto.getPlatform()), dto.getAgentId(),
                    dto.getTraceId(), dto.getCurrency(), this.findVendorCurrencyCode(dto.getCurrency(), dto.getVendorId()));


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
