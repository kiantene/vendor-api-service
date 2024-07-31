package com.nextgen.gameaggregator.vendor.jili.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.vendor.jili.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setHttpMethod(HttpMethod.GET);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(EndPoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String agentId = Optional.ofNullable(credentials.get(Credentials.AGENT_ID))
                .orElseThrow(InvalidVendorLineException::new);

        String agentKey = Optional.ofNullable(credentials.get(Credentials.AGENT_KEY))
                .orElseThrow(InvalidVendorLineException::new);

        VendorService service = new VendorService();
        service.setAgentId(agentId);
        service.setAgentKey(agentKey);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("Token", gameSession.getToken());
        formData.add("GameId", gameSession.getVendorGameCode());
        formData.add("Lang", gameSession.getVendorLanguageCode());
        formData.add("AgentId", agentId);
        String key = service.keyGenerator(formData);
        formData.add("Key", key);
        formData.add("HomeUrl", gameSession.getLobbyUrl());

        return formData;
    }
}
