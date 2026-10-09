package com.smartkrishi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class WeatherCacheConfig {

    public static final String CACHE_WEATHER_CITY = "weatherByCity";
    public static final String CACHE_WEATHER_COORDINATES = "weatherByCoordinates";
    public static final String CACHE_WEATHER_CITY_STALE = "weatherStaleByCity";
    public static final String CACHE_WEATHER_COORDINATES_STALE = "weatherStaleByCoordinates";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Default specification for dynamic caches (10 minutes)
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(500));

        // Pre-register fresh 10-minute caches
        cacheManager.registerCustomCache(CACHE_WEATHER_CITY,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .maximumSize(500)
                        .build());

        cacheManager.registerCustomCache(CACHE_WEATHER_COORDINATES,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .maximumSize(500)
                        .build());

        // Pre-register 6-hour stale fallback caches
        cacheManager.registerCustomCache(CACHE_WEATHER_CITY_STALE,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(6))
                        .maximumSize(1000)
                        .build());

        cacheManager.registerCustomCache(CACHE_WEATHER_COORDINATES_STALE,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(6))
                        .maximumSize(1000)
                        .build());

        return cacheManager;
    }
}
