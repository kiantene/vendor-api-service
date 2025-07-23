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
            @Qualifier("agentApiCredentialCache") Collection agentApiCredentialCache,
            @Qualifier("agentPlayerCache") Collection agentPlayerCache,
            @Qualifier("vendorPlayerCache") Collection vendorPlayerCache,
            @Qualifier("currencyCache") Collection currencyCache,
            @Qualifier("vendorCache") Collection vendorCache,
            @Qualifier("vendorGameCache") Collection vendorGameCache,
            @Qualifier("gameCategoryCache") Collection gameCategoryCache
    ) {
        this.collectionMap = Map.of(
                AgentApiCredential.class, agentApiCredentialCache,
                AgentPlayer.class, agentPlayerCache,
                VendorPlayer.class, vendorPlayerCache,
                Currency.class, currencyCache,
                Vendor.class, vendorCache,
                VendorGame.class, vendorGameCache,
                GameCategory.class, gameCategoryCache
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
