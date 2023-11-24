package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.Agent;
import com.nextgen.gameaggregator.entity.VendorGameDeactivated;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.repository.VendorGameDeactivatedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VendorGameDeactivatedService {

    @Autowired
    private VendorGameDeactivatedRepository vendorGameDeactivatedRepository;
    public boolean checkGameSupported(Agent agent, Integer vendorGameId) throws DisabledGameException {
        Integer gameNotDeletedFromVendorGameDeactivated = 0;
        Integer sasEntityHierarchyId = 1;
        VendorGameDeactivated vendorGameDeactivated = vendorGameDeactivatedRepository.findByVendorGameIdAndAgentIdAndAgentMasterIdAndHouseIdAndSasEntityHierarchyIdAndIsDeleted(vendorGameId, agent.getId(),
                agent.getMasterAgentId(), agent.getHouseId(), gameNotDeletedFromVendorGameDeactivated, sasEntityHierarchyId);

        if (vendorGameDeactivated != null) {
            throw new DisabledGameException();
        }

        return false;

    }

}
