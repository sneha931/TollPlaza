package com.freightfox.tollplaza.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's cache abstraction. The Caffeine cache names and spec are set in
 * application.properties. Kept separate from the main class so web-slice tests don't need a
 * CacheManager.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
