package com.nextgen.gameaggregator.controller;

import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.repository.AgentApiCredentialRepository;
import com.nextgen.gameaggregator.repository.AgentPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class ControllerServices {

    @CacheEvict(value = "AgentApiCredentials", allEntries = true, cacheManager = "cacheManager")
    public void clearAgentApiCredentials(){
    }

    @CacheEvict(value = "AgentPlayers", allEntries = true, cacheManager = "cacheManager")
    public void clearAgentPlayers(){
    }

    @CacheEvict(value = "VendorGames", allEntries = true, cacheManager = "cacheManager")
    public void clearVendorGames(){
    }

    @CacheEvict(value = "VendorLines", allEntries = true, cacheManager = "cacheManager")
    public void clearVendorLines(){
    }
}
