package org.bitly.util;

import redis.clients.jedis.Jedis;

public class RedisTest {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("127.0.0.1", 6379); // Make sure to match your host and port
        try {
            jedis.connect();
            System.out.println("Connected to Redis successfully!");
            jedis.close();
        } catch (Exception e) {
            System.out.println("Failed to connect to Redis: " + e.getMessage());
        }
    }
}

