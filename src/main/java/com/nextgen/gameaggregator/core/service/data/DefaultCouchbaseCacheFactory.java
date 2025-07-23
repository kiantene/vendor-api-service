package com.nextgen.gameaggregator.core.service.data;

import com.couchbase.client.java.Collection;
import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.gameaggregator.entity.ga.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultCouchbaseCacheFactory implements CouchbaseCacheFactory {

    private final Map<Class<?>, Collection> collectionMap;

    public DefaultCouchbaseCacheFactory(
            @Qualifier("agentApiCredentialCollection") Collection agentApiCredentialCollection,
            @Qualifier("agentPlayerCache") Collection agentPlayerCollection,
            @Qualifier("vendorPlayerCache") Collection vendorPlayerCollection
    ) {
        this.collectionMap = Map.of(
                AgentApiCredential.class, agentApiCredentialCollection,
                AgentPlayer.class, agentPlayerCollection,
                VendorPlayer.class, vendorPlayerCollection
        );
    }

    @Override
    public Collection get(Class<?> clazz) {
        Collection collection = collectionMap.get(clazz);
        if (collection == null) {
            throw new IllegalArgumentException("No Couchbase collection registered for class: " + clazz.getSimpleName());
        }
        return collection;
    }
}
