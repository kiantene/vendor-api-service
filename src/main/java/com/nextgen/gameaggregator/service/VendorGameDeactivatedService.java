package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.VendorGameDeactivated;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.repository.VendorGameDeactivatedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class VendorGameDeactivatedService {

    @Autowired
    private VendorGameDeactivatedRepository vendorGameDeactivatedRepository;

    @Cacheable(value = "VendorGameDeactivated", key = "{#agentId, #vendorGameId}", cacheManager = "cacheManager")
    public VendorGameDeactivated checkGameSupported(Integer agentId, Integer vendorGameId) throws DisabledGameException {
        Integer gameNotDeletedFromVendorGameDeactivated = 0;

        //TODO CHECK AGAINST PARENT MASTER AGENT, HOUSE, AND SUPER ADMIN DISABLE GAME
        //check against agent level first
        VendorGameDeactivated vendorGameDeactivated = vendorGameDeactivatedRepository.findByVendorGameIdAndAgentIdAndIsDeleted(vendorGameId, agentId, gameNotDeletedFromVendorGameDeactivated);

        if (vendorGameDeactivated == null) {
            throw new DisabledGameException();
        }

        return vendorGameDeactivated;
    }

}
