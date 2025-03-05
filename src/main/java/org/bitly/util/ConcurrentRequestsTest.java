package org.bitly.util;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;

public class ConcurrentRequestsTest {

    private static final String BASE_URL = "http://localhost:8090/api";  // Update with your actual server URL
    private static final int NUM_REQUESTS = 10;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_REQUESTS);
        List<Long> responseTimes = new ArrayList<>();

        // Create 10 POST and GET requests (for both /shorten and /redirect)
        for (int i = 0; i < NUM_REQUESTS; i++) {
            // Sending POST request to /shorten
            executorService.submit(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    sendPostRequest("/shorten", "{\"url\": \"https://example.com\"}");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                long endTime = System.currentTimeMillis();
                responseTimes.add(endTime - startTime);
            });

            // Sending GET request to /redirect with a dummy code
            executorService.submit(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    sendGetRequest("/redirect?code=LyKpO9");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                long endTime = System.currentTimeMillis();
                responseTimes.add(endTime - startTime);
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // Calculate percentiles
        calculatePercentiles(responseTimes);
    }

    private static void sendPostRequest(String endpoint, String jsonPayload) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void sendGetRequest(String endpoint) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .GET()
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void calculatePercentiles(List<Long> responseTimes) {
        Collections.sort(responseTimes);

        // p50 = 50th percentile
        long p50 = responseTimes.get((int) (responseTimes.size() * 0.50));
        // p90 = 90th percentile
        long p90 = responseTimes.get((int) (responseTimes.size() * 0.90));
        // p95 = 95th percentile
        long p95 = responseTimes.get((int) (responseTimes.size() * 0.95));
        // p99 = 99th percentile
        long p99 = responseTimes.get((int) (responseTimes.size() * 0.99));

        System.out.println("p50: " + p50 + "ms");
        System.out.println("p90: " + p90 + "ms");
        System.out.println("p95: " + p95 + "ms");
        System.out.println("p99: " + p99 + "ms");
    }
}

