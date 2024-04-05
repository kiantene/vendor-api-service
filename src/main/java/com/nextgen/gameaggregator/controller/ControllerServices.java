package com.nextgen.gameaggregator.controller;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class ControllerServices {

    @CacheEvict(value = "AgentApiCredentials", allEntries = true, cacheManager = "cacheManager")
    public void clearAgentApiCredentials(){
    }

    @CacheEvict(value = "AgentApiCredentialsByApiKey", allEntries = true, cacheManager = "cacheManager")
    public void clearAgentApiCredentialsByApiKey(){
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
