package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignResolveStrategy;
import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignService;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class CampaignDataService {
    private final CampaignCacheService cache;
    private final CampaignService repository;

    public CampaignDataService(CampaignCacheService cache, CampaignService repository) {
        this.cache = cache;
        this.repository = repository;
    }

    public Campaign get(String vendorCampaignCode, Integer vendorLineId, Integer promoType) {
        return Optional.ofNullable(this.cache.getByVendorCampaignCodeAndVendorLineIdAndPromoType(vendorCampaignCode, vendorLineId, promoType))
                .orElseThrow(() -> new EntityNotFoundException(Campaign.class, "vendorCampaignCodeAndVendorLineIdAndPromoType", vendorCampaignCode, vendorLineId, promoType));
    }

    public Campaign getByPlayerUuid(String playerUuid) {
        return Optional.ofNullable(this.cache.getByPlayerUuid(playerUuid))
                .orElseThrow(() -> new EntityNotFoundException(Campaign.class, "playerUuid", playerUuid));
    }

    public Campaign getByRef(CampaignResolveStrategy strategy, Map<String, String> params) {
        return Optional.ofNullable(this.cache.getByRef(strategy, params))
                .orElseThrow(() -> new EntityNotFoundException(Campaign.class, "resolveCampaign", strategy, params));
    }

    public Campaign createPayoutCampaign(Integer promoType, Integer vendorId, Integer vendorLineId, String vendorCampaignCode, String vendorCampaignName, String vendorTransactionId, String currencyCode, String vendorCurrencyCode, Long payoutTime) {
        Supplier<Campaign> create = () -> repository.createPayoutCampaign(promoType, vendorId, vendorLineId, vendorCampaignCode, vendorCampaignName, vendorTransactionId, currencyCode, vendorCurrencyCode, payoutTime);
        return StringUtils.hasText(vendorCampaignCode)
                ? cache.createPayoutCampaign(promoType, vendorLineId, vendorCampaignCode, create)
                : create.get();
    }
}
