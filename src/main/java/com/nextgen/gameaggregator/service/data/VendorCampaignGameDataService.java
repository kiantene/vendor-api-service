package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.ga.VendorCampaignGame;
import com.nextgen.gameaggregator.repository.ga.reader.VendorCampaignGameReaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorCampaignGameDataService {

    private final VendorCampaignGameReaderRepository vendorCampaignGameReaderRepository;

    @Cacheable(value = "VendorCampaignGame", key = "#vendorGameCode", cacheManager = "cacheManager")
    public Integer getIsSupportFreeRound(String vendorGameCode) {
        VendorCampaignGame game = vendorCampaignGameReaderRepository.findByVendorGameCode(vendorGameCode);
        return game.getIsSupportFreeRound();
    }
}
