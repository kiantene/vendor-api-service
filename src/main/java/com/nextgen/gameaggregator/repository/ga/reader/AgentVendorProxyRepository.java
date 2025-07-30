package com.nextgen.gameaggregator.repository.ga.reader;

import com.nextgen.gameaggregator.entity.ga.AgentVendorProxy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentVendorProxyRepository extends JpaRepository<AgentVendorProxy, Integer> {

    List<AgentVendorProxy> findAllByAgentIdAndVendorId(Integer agentId, Integer vendorId);
}
