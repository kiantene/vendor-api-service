package com.nextgen.gameaggregator.core.service.data;

import com.couchbase.client.java.Collection;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CacheCollectionFactory {
    private final Map<Class<?>, Collection> collectionMap;

    public CacheCollectionFactory(
            @Qualifier("agentPlayerCollection") Collection agentPlayerCollection,
            @Qualifier("vendorPlayerCollection") Collection vendorPlayerCollection,
            @Qualifier("agentApiCredentialCollection") Collection agentApiCredentialCollection
    ) {
        this.collectionMap = Map.of(
                AgentPlayer.class, agentPlayerCollection,
                VendorPlayer.class, vendorPlayerCollection,
                AgentApiCredential.class, agentApiCredentialCollection
        );
    }

    public Collection get(Class<?> clazz) {
        Collection collection = collectionMap.get(clazz);
        if (collection == null) {
            throw new IllegalArgumentException("No Couchbase collection registered for class: " + clazz.getSimpleName());
        }
        return collection;
    }
}
