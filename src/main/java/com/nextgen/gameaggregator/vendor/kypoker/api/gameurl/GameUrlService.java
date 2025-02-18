package com.nextgen.gameaggregator.vendor.kypoker.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.kypoker.constant.Actions;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@Slf4j
@Getter
@Setter
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    @Autowired
    private WalletService walletService;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setHttpMethod(HttpMethod.GET);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String TimeStamp = String.valueOf(System.currentTimeMillis());
        String AgentId = (ValidationUtils.validateCredential(credentials.get(Credentials.AGENT_ID)));
        String AesKey = (ValidationUtils.validateCredential(credentials.get(Credentials.AES_KEY)));
        String Md5Key = (ValidationUtils.validateCredential(credentials.get(Credentials.MD5_KEY)));
        String Param;

        MultiValueMap<String, String> encryptParam = new LinkedMultiValueMap<>();
        encryptParam.add("s", Actions.LOGIN);
        encryptParam.add("account", gameSession.getVendorPlayerUsername());
        encryptParam.add("orderid", AgentId+TimeStamp+gameSession.getVendorPlayerUsername());
        encryptParam.add("ip", "103.22.180.95");
        encryptParam.add("lineCode", String.valueOf(gameSession.getVendorLineId()));
        encryptParam.add("KindId", String.valueOf(gameSession.getVendorGameCode()));
        encryptParam.add("currency", String.valueOf(gameSession.getVendorCurrencyCode()));

        String queryString = UriComponentsBuilder.fromPath("")
                .queryParams(encryptParam)
                .build()
                .encode()
                .toString()
                .replaceFirst("^\\?", ""); // Remove leading "?"

        try {
            Param = VendorService.AESEncrypt(queryString, AesKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("agent", AgentId);
        param.add("timestamp", String.valueOf(System.currentTimeMillis()));
        param.add("param", Param);
        param.add("key", VendorService.MD5Encrypt(AgentId+TimeStamp+Md5Key));

        return param;
    }

}





