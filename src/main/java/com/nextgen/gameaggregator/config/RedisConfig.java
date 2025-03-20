package com.nextgen.gameaggregator.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
@EnableRedisRepositories
@EnableCaching
@Component
public class RedisConfig extends CachingConfigurerSupport {

    @Value("${spring.data.redis.database}")
    private Integer REDIS_DATABASE;
    @Value("${spring.data.redis.host}")
    private String REDIS_HOST;
    @Value("${spring.data.redis.port}")
    private Integer REDIS_PORT;
    @Value("${spring.data.redis.username}")
    private String REDIS_USERNAME;
    @Value("${spring.data.redis.password}")
    private String REDIS_PASSWORD;
    @Value("${spring.data.redis.mode}")
    private RedisMode mode;
    @Value("${spring.data.redis.nodehosts}")
    private List<String> nodeHosts;

    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }

    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        if (mode == RedisMode.CLUSTER) {
            RedisClusterConfiguration configuration = new RedisClusterConfiguration(nodeHosts);
            configuration.setUsername(REDIS_USERNAME);
            configuration.setPassword(REDIS_PASSWORD);
            return new JedisConnectionFactory(configuration);
        } else {
            // } else if (mode == RedisMode.STANDALONE) {
            RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(REDIS_HOST,
                    REDIS_PORT);
            configuration.setUsername(REDIS_USERNAME);
            configuration.setPassword(REDIS_PASSWORD);
            configuration.setDatabase(REDIS_DATABASE);
            return new JedisConnectionFactory(configuration);
        }
        // throw new IllegalArgumentException("Redis mode cannot be null!");
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        return mapper;
    }

    @Bean
    public Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer(ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper redisObjectMapper = objectMapper.copy();
        // Enable default typing only for Redis serialization
        redisObjectMapper.activateDefaultTyping(redisObjectMapper.getPolymorphicTypeValidator(), 
            ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(redisObjectMapper);
        return serializer;
    }

    @Bean
    @Primary
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory, 
            Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        redisTemplate.setEnableTransactionSupport(true);
        return redisTemplate;
    }

    @Bean
    @Primary
    CacheManager cacheManager(RedisConnectionFactory factory, Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer) {
        RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer);

        Map<String, RedisCacheConfiguration> cacheNamesConfigurationMap = new HashMap<>();
    
        cacheNamesConfigurationMap.put("sportSettledBet", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorGameDeactivated", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(3)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("RawBetIdempotentLog", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(3)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("Agent", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("Vendors", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorLanguages", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorCurrency", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorCurrencyCode", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("Languages", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("Platforms", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("AgentApiCredentialsByApiKey", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("AgentCurrency", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));

        // AgentCurrencies is deprecated, use AgentCurrency instead
        cacheNamesConfigurationMap.put("AgentCurrencies", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));

        cacheNamesConfigurationMap.put("Currencies", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("CurrencyCode", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorCurrencies", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("AgentVendorLine", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("AgentVendorLines", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(3)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorGameCode", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorGameCurrency", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("BetNotFoundLog", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("rawResultLog", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(20)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("SettledBet", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("ResultBet", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("UnsettledBet", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("UnsettledBetTop1", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("UnsettledBetByETID", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("GameSessions", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorLines", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("BetHistories", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(10)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorLineCredentials", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("AgentPlayers", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorPlayers", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorPlayerUsername", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorGame", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("VendorGames", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(3)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("ProductGameCodes", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(3)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("AgentApiCredentials", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("promoData", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("playerBalance", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(10)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("rawBetRefundLogs", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("HighestPriorityLine", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("ProductDetailByGameCategoryAndCurrency", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("ProductGameDeactivated", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("AgentSupportedCurrency", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("ProductVendorGameCode", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("WalletTransaction", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)).serializeValuesWith(pair));

        // EvoPlay Increase Balance EndPoints use
        cacheNamesConfigurationMap.put("EvoPlayBalance", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));

        //region transfer wallet
        cacheNamesConfigurationMap.put("TraceIds", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)).serializeValuesWith(pair));

        cacheNamesConfigurationMap.put("RawTransferHistories", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)).serializeValuesWith(pair));

        cacheNamesConfigurationMap.put("AccessKeys", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));

        cacheNamesConfigurationMap.put("GameCategories", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("RequestIdempotentLog", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)).serializeValuesWith(pair));
        cacheNamesConfigurationMap.put("ClickhouseFirstDataOfCurrencyConversionChanges", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(30)).serializeValuesWith(pair));
        //endregion

        return new RedisCacheManager(RedisCacheWriter.nonLockingRedisCacheWriter(factory),
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)),
                cacheNamesConfigurationMap);
    }

    public enum RedisMode {
        CLUSTER,
        STANDALONE
    }
}
