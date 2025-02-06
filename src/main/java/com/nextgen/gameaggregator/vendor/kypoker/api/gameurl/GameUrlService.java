package com.nextgen.gameaggregator.vendor.kypoker.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
@Slf4j
@Getter
@Setter
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    @Autowired
    private WalletService walletService;

    private String apiUrl;
    private String merchantId;
    private String merchantKey;
    private String AgentId;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(EndPoints.LAUNCH_GAME);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        this.setApiUrl(ValidationUtils.validateCredential(credentials.get(Credentials.API_URL)));
        this.setAgentId(ValidationUtils.validateCredential(credentials.get(Credentials.AGENT_ID)));

        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("agent", this.AgentId);
        param.add("timestamp", String.valueOf(System.currentTimeMillis()));
        param.add("param", gameSession.getVendorPlayerUsername());
        param.add("currency", gameSession.getCurrencyCode());
        param.add("session_id", gameSession.getToken());

        return param;
    }

}





