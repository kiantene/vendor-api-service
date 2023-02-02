package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorPlayerRepository extends JpaRepository<VendorPlayer, Long> {
    VendorPlayer findByAgentPlayerIdAndVendorLineIdAndCurrencyId(Long agentPlayerId, Integer vendorLineId, Integer currencyId);
    VendorPlayer findByUsername(String username);
}
