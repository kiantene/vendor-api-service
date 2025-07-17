package com.nextgen.gameaggregator.vendor.aviatorstudio.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;

import java.util.Map;

public class GameUrlService extends BaseGameUrlService<GameUrlVo> {
    @Autowired
    private S3Service s3Service;

    protected GameUrlService(Class<GameUrlVo> responseVoClass) {
        super(responseVoClass);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        return null;
    }
}
