package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.VendorGameDeactivated;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameDeactivatedRepository;
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
                agent.getMasterAgentId(), agent.getHouseId(), gameNotDeletedFromVendorGameDeactivated);

        if (vendorGameDeactivated != null) {
            throw new DisabledGameException();
        }

        return false;

    }

}
