package de.innologic.templateservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TEMPLATE_RESOLVE_CACHE = "template-resolve-cache";
    public static final String TEMPLATE_APPROVED_VERSION_CACHE = "template-approved-version-cache";

    @Bean
    CacheManager cacheManager(@Value("${template.cache.ttl-seconds:120}") long ttlSeconds) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                TEMPLATE_RESOLVE_CACHE,
                TEMPLATE_APPROVED_VERSION_CACHE
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(10_000)
                .recordStats());
        return cacheManager;
    }
}

