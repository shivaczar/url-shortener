package org.bitly.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.bitly.entity.RecentShortenedUrl;
import org.bitly.entity.UrlMapping;
import org.bitly.entity.User;
import org.bitly.repository.UrlRepository;
import org.bitly.repository.UserRepository;
import org.bitly.service.RecentShortenedUrlService;
import org.bitly.service.UrlShortenerService;
import org.bitly.util.NUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/urls")
public class UrlShortenerController {

    @Autowired
    private UrlShortenerService urlShortenerService;

    @Autowired
    private RecentShortenedUrlService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UrlRepository urlRepository;

    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);


    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shortenUrl(@RequestBody Map<String, String> request,
                                                          @RequestHeader("X-API-KEY") String apiKey) {
        String originalUrl = request.get("url");
        String customCode = request.get("customCode"); // Optional
        String expiryDateStr = request.get("expiryDate"); // Optional
        String password = request.get("password");

        if (originalUrl == null || originalUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }

        LocalDateTime expiryDate = null;
        if (expiryDateStr != null && !expiryDateStr.isBlank()) {
            expiryDate = LocalDateTime.parse(expiryDateStr);
        }

        try {
            String shortCode = urlShortenerService.shortenUrl(originalUrl, apiKey, customCode, expiryDate, password);
            return ResponseEntity.ok(Map.of("shortCode", shortCode));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/shorten/batch")
    public ResponseEntity<Object> shortenUrls(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestBody Map<String, List<Map<String, String>>> request, HttpServletRequest httpRequest) {

        List<Map<String, String>> urlRequests = request.get("urls");

        if (urlRequests == null || urlRequests.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URLs list cannot be empty"));
        }

        List<Map<String, String>> shortenedUrls = urlShortenerService.shortenUrls(urlRequests, httpRequest);
        return ResponseEntity.ok(shortenedUrls);
    }

    @PutMapping("/shorten/{shortCode}/expiry")
    public ResponseEntity<String> updateExpiry(@PathVariable String shortCode,
                                               @RequestParam String apiKey,
                                               @RequestParam String expiryDate) {
        try {
            LocalDateTime newExpiryDate = LocalDateTime.parse(expiryDate);
            urlShortenerService.updateExpiry(shortCode, apiKey, newExpiryDate);
            return ResponseEntity.ok("Expiry date updated successfully.");
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(400).body("Invalid expiry date format.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }




    @GetMapping("/redirect")
    public ResponseEntity<Object> redirectToOriginalUrl(@RequestParam String code,
                                                        @RequestParam(required = false) String password) {
        Optional<UrlMapping> urlMappingOpt = urlShortenerService.getUrlMapping(code);

        if (urlMappingOpt.isEmpty()) {
            logger.warn("Short code {} not found or expired", code);
            return ResponseEntity.status(404).body("Short code not found or expired");
        }

        UrlMapping urlMapping = urlMappingOpt.get();

        // Check if a password is required
        if (urlMapping.getPassword() != null) {
            if (password == null || password.isBlank()) {
                return ResponseEntity.status(401).body("Password required for this short code");
            }

            if (!NUtil.verifyPassword(password, urlMapping.getPassword())) {
                return ResponseEntity.status(401).body("Invalid password");
            }
        }

        // Update analytics (click count + last accessed time)
        urlShortenerService.incrementClick(code);
        logger.info("Redirecting short code {} to {}", code, urlMapping.getOriginalUrl());

        return ResponseEntity.status(302).location(URI.create(urlMapping.getOriginalUrl())).build();

    }


    @DeleteMapping("delete/{shortCode}")
    public ResponseEntity<Map<String, String>> deleteShortCode(
            @PathVariable String shortCode,
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestParam(required = false) String password) {

        boolean deleted = urlShortenerService.deleteUrl(shortCode, apiKey, password);

        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Short code deleted successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Unauthorized or incorrect password for deletion"));
        }
    }


    @GetMapping("/recent")
    public List<RecentShortenedUrl> getRecentUrls() {
        return service.getRecentUrls();
    }


    @GetMapping("/top-clicked")
    public ResponseEntity<List<Map<String, Object>>> getTopClickedUrls() {
        return ResponseEntity.ok(urlShortenerService.getTop10MostClickedUrls());
    }

    @GetMapping("/top-shortened")
    public ResponseEntity<List<Map<String, Object>>> getTopShortenedUrls() {
        return ResponseEntity.ok(urlShortenerService.getTop10MostShortenedUrls());
    }


    @GetMapping("/all")
    public ResponseEntity<List<UrlMapping>> getAllUrls() {
        List<UrlMapping> urls = urlShortenerService.getAllUrls();
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/user")
    public ResponseEntity<Object> getAllUrls(@RequestHeader("X-API-KEY") String apiKey) {
        Optional<User> userOpt = userRepository.findByApiKey(apiKey);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid API Key"));
        }

        List<UrlMapping> urls = urlRepository.findByUserIdAndIsDeletedFalse(userOpt.get().getId());

        List<Map<String, Object>> response = urls.stream()
                .map(url -> Map.<String, Object>of(  // Explicit type declaration
                        "shortCode", url.getShortCode(),
                        "originalUrl", url.getOriginalUrl(),
                        "expiryDate", url.getExpiryDate() != null ? url.getExpiryDate().toString() : "Never",
                        "clicks", url.getClickCount(),
                        "passwordProtected", url.getPassword() != null  // Ensure type consistency
                ))
                .toList();

        return ResponseEntity.ok(response);
    }


    @GetMapping("/hello")
    public String index() {
        return "Greetings from Spring Boot!";
    }

}

