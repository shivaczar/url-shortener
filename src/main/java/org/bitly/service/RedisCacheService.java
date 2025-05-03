package org.bitly.service;

import jakarta.annotation.PostConstruct;
import redis.clients.jedis.Jedis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RedisCacheService {

    private Jedis jedis;

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    private final Map<String, String> apiKeyToPlan = Map.of(
            "api-key-free", "free",
            "api-key-hobby", "hobby",
            "api-key-enterprise", "enterprise"
    );

    public RedisCacheService() {
        // Constructor should not use redisHost and redisPort yet
    }

    @PostConstruct
    public void init() {
        System.out.println("Initializing Redis connection to " + redisHost + ":" + redisPort);
        this.jedis = new Jedis(redisHost, redisPort);
    }

    public String getCachedUrl(String shortCode) {
        try {
            String cachedUrl = jedis.get(shortCode);
            System.out.println("getting from redis :: "+ cachedUrl);
            return cachedUrl;
        } catch (Exception e) {
            System.out.println("Error During retrieval of url: " + e.getMessage());
            return null;
        }
    }

    public void cacheUrl(String shortCode, String originalUrl, long ttlInSeconds) {
        try {
            jedis.set(shortCode, originalUrl);
            jedis.expire(shortCode, (int) ttlInSeconds); // Custom TTL
        } catch (Exception e) {
            System.out.println("Error during caching the url: " + e.getMessage());
        }
    }

    public boolean isAllowed(String ip, String path, int maxRequestsPerSec) {
        String key = "rate:" + ip + ":" + path;
        try {
            long count = jedis.incr(key);
            if (count == 1) {
                jedis.expire(key, 1); // 1 second window
            }
            return count <= maxRequestsPerSec;
        } catch (Exception e) {
            System.out.println("Rate limit error: " + e.getMessage());
            return true; // fallback to allow in case of Redis error
        }
    }

    public boolean isAllowedByApiKey(String apiKey, String path, int maxRequestsPerSec) {
        String key = "rate:" + apiKey + ":" + path;
        try {
            long count = jedis.incr(key);
            if (count == 1) {
                jedis.expire(key, 1); // Set TTL of 1 second
            }
            return count <= maxRequestsPerSec;
        } catch (Exception e) {
            System.out.println("Rate limiting failed: " + e.getMessage());
            return true; // Allow in case of Redis issues
        }
    }


    public boolean isAllowedByUserPlan(String apiKey, String path) {
        String plan = apiKeyToPlan.getOrDefault(apiKey, "free");

        int maxRequests;
        int windowSeconds;

        switch (plan) {
            case "enterprise":
                maxRequests = 100;
                windowSeconds = 1;
                break;
            case "hobby":
                maxRequests = 10;
                windowSeconds = 1;
                break;
            case "free":
            default:
                maxRequests = 5;
                windowSeconds = 60;
                break;
        }

        String key = String.format("rate:%s:%s", apiKey, path);
        try {
            long count = jedis.incr(key);
            if (count == 1) {
                jedis.expire(key, windowSeconds);
            }
            return count <= maxRequests;
        } catch (Exception e) {
            System.out.println("Redis rate limit error: " + e.getMessage());
            return true; // fallback to allow if Redis is down
        }
    }

    public long increment(String key, int ttlSeconds) {
        long count = jedis.incr(key);
        if (count == 1) {
            jedis.expire(key, ttlSeconds);
        }
        return count;
    }

    public int ttl(String key) {
        Long ttl = jedis.ttl(key);
        return ttl > 0 ? ttl.intValue() : 0;
    }




}


