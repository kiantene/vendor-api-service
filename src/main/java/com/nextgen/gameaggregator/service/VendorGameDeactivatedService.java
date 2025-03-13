package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.VendorGameDeactivated;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameDeactivatedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VendorGameDeactivatedService {
    private final VendorGameDeactivatedRepository vendorGameDeactivatedRepository;

    public VendorGameDeactivatedService(VendorGameDeactivatedRepository vendorGameDeactivatedRepository) {
        this.vendorGameDeactivatedRepository = vendorGameDeactivatedRepository;
    }

    public boolean checkGameSupported(Integer houseId, Integer masterAgentId, Integer agentId, Integer vendorGameId) throws DisabledGameException {
        Integer gameNotDeletedFromVendorGameDeactivated = 0;

        // TODO: refactor sql
        VendorGameDeactivated vendorGameDeactivated = vendorGameDeactivatedRepository.findByVendorGameIdAndAgentIdAndAgentMasterIdAndHouseIdAndSasEntityHierarchyIdAndIsDeleted(vendorGameId, agentId,
                masterAgentId, houseId, gameNotDeletedFromVendorGameDeactivated);

        if (vendorGameDeactivated != null) {
            throw new DisabledGameException();
        }

        return false;

    }

}
