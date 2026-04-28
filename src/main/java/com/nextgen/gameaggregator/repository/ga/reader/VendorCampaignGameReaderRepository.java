package com.nextgen.gameaggregator.repository.ga.reader;

import com.nextgen.gameaggregator.entity.ga.VendorCampaignGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorCampaignGameReaderRepository extends JpaRepository<VendorCampaignGame, Integer> {
    VendorCampaignGame findByVendorGameCode(String vendorGameCode);
}
