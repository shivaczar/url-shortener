package org.example.controller;

import org.example.service.UrlShortenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UrlShortenerController {

    @Autowired
    private UrlShortenerService urlShortenerService;

    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);

    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shortenUrl(@RequestBody Map<String, String> request) {
        String originalUrl = request.get("url");
        if (originalUrl == null || originalUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }

        String shortCode = urlShortenerService.shortenUrl(originalUrl);
        return ResponseEntity.ok(Map.of("shortCode", shortCode));
    }

    @GetMapping("/redirect")
    public ResponseEntity<Object> redirectToOriginalUrl(@RequestParam String code) {
        Optional<String> originalUrl = urlShortenerService.getOriginalUrl(code);

        if (originalUrl.isPresent()) {
            logger.info("Redirecting short code {} to {}", code, originalUrl.get());
            return ResponseEntity.status(302).location(URI.create(originalUrl.get())).build();
        } else {
            logger.warn("Short code {} not found", code);
            return ResponseEntity.status(404).body("Short code not found");
        }
    }

    @DeleteMapping("/delete/{shortCode}")
    public ResponseEntity<String> deleteShortCode(@PathVariable String shortCode) {
        boolean deleted = urlShortenerService.deleteShortCode(shortCode);
        if (deleted) {
            return ResponseEntity.ok("Short code deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Short code not found.");
        }
    }

    @GetMapping("/hello")
    public String index() {
        return "Greetings from Spring Boot!";
    }
}

