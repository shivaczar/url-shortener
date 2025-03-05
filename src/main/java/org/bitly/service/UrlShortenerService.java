package org.bitly.service;

import jakarta.transaction.Transactional;
import org.bitly.entity.UrlMapping;
import org.bitly.entity.User;
import org.bitly.repository.UrlRepository;
import org.bitly.repository.UserRepository;
import org.bitly.util.NUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import jakarta.annotation.Nullable;


import java.time.LocalDateTime;
import java.util.*;

@Service
public class UrlShortenerService {

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;

    // Method to shorten the URL
    public String shortenUrlOld(String originalUrl) {
        // Check if the URL already exists in the database
        Optional<UrlMapping> existingUrlMapping = urlRepository.findByOriginalUrl(originalUrl);

        if (existingUrlMapping.isPresent()) {
            // If the URL already exists, return the existing short code
            return existingUrlMapping.get().getShortCode();
        } else {
            // If the URL is not in the database, generate a new short code
            String shortCode;
            do {
                shortCode = generateShortCode();
            } while (urlRepository.findByShortCode(shortCode).isPresent()); // Ensure the short code is unique

            // Save the new URL mapping
            UrlMapping urlMapping = new UrlMapping(shortCode, originalUrl, 10L, null, null);
            urlRepository.save(urlMapping);
            return shortCode;
        }
    }

    public String shortenUrl(String originalUrl, String apiKey, @Nullable String customCode,
                             @Nullable LocalDateTime expiryDate, @Nullable String password) {
        User user = userRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API Key"));

        String shortCode;
        if (customCode != null && !customCode.isBlank()) {
            if (urlRepository.findByShortCodeAndIsDeletedFalse(customCode).isPresent()) {
                throw new RuntimeException("Custom short code is already taken");
            }
            shortCode = customCode;
        } else {
            do {
                shortCode = generateShortCode();
            } while (urlRepository.findByShortCodeAndIsDeletedFalse(shortCode).isPresent());
        }

        // Hash password if provided
        String hashedPassword = (password != null && !password.isBlank()) ?  NUtil.hashPassword(password): null;

        UrlMapping urlMapping = new UrlMapping(shortCode, originalUrl, user.getId(), expiryDate, hashedPassword);

        // Store hashed password if provided
        if (password != null && !password.isBlank()) {
            urlMapping.setPassword(NUtil.hashPassword(password));
        }

        urlRepository.save(urlMapping);
        return shortCode;
    }



    public List<Map<String, String>> shortenUrls(List<Map<String, String>> urlRequests, String apiKey) {
        User user = userRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API Key"));

        // Check if user is allowed to perform bulk shortening
        if (!"enterprise".equalsIgnoreCase(user.getTier())) {
            throw new RuntimeException("Bulk URL creation is only available for enterprise users.");
        }

        List<Map<String, String>> results = new ArrayList<>();

        for (Map<String, String> request : urlRequests) {
            String originalUrl = request.get("originalUrl");
            String customCode = request.get("customCode");  // Allow custom short code
            String password = request.get("password");  // Optional password field
            LocalDateTime expiryDate = request.containsKey("expiry_date") ?
                    LocalDateTime.parse(request.get("expiry_date")) : null;

            String shortCode;
            if (customCode != null && !customCode.isBlank()) {
                // Validate if custom code is already taken
                if (urlRepository.findByShortCodeAndIsDeletedFalse(customCode).isPresent()) {
                    results.add(Map.of(
                            "originalUrl", originalUrl,
                            "error", "Custom short code '" + customCode + "' is already taken"
                    ));
                    continue; // Skip saving this entry
                }
                shortCode = customCode;
            } else {
                // Generate a random short code if none is provided
                do {
                    shortCode = generateShortCode();
                } while (urlRepository.findByShortCodeAndIsDeletedFalse(shortCode).isPresent());
            }

            // Hash password if provided
            String hashedPassword = (password != null && !password.isBlank()) ?  NUtil.hashPassword(password): null;

            // Save mapping
            UrlMapping urlMapping = new UrlMapping(shortCode, originalUrl, user.getId(), expiryDate, hashedPassword);
            urlRepository.save(urlMapping);

            results.add(Map.of(
                    "originalUrl", originalUrl,
                    "shortCode", shortCode,
                    "expiry_date", expiryDate != null ? expiryDate.toString() : "Never",
                    "password_protected", hashedPassword != null ? "Yes" : "No"
            ));
        }

        return results;
    }


    public void updateExpiry(String shortCode, String apiKey, LocalDateTime newExpiryDate) {
        User user = userRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API Key"));

        UrlMapping urlMapping = urlRepository.findByShortCodeAndIsDeletedFalse(shortCode)
                .orElseThrow(() -> new RuntimeException("Short code not found"));

        // Ensure the user owns the short code
        if (!urlMapping.getUserId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to edit this short code.");
        }

        // Update expiry date
        urlMapping.setExpiryDate(newExpiryDate);
        urlRepository.save(urlMapping);
    }





    // Method to get the original URL by short code
    public Optional<String> getOriginalUrl(String shortCode) {
        return urlRepository.findByShortCodeAndIsDeletedFalse(shortCode)
                .filter(urlMapping -> urlMapping.getExpiryDate() == null || urlMapping.getExpiryDate().isAfter(LocalDateTime.now()))
                .map(UrlMapping::getOriginalUrl);
    }


    // Method to generate a random short code
    private String generateShortCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public boolean deleteShortCode(String shortCode) {
        Optional<UrlMapping> urlMapping = urlRepository.findByShortCode(shortCode);
        if (urlMapping.isPresent()) {
            urlRepository.delete(urlMapping.get());
            return true;
        }
        return false;  // Short code not found
    }

    @Transactional
    public void incrementClick(String shortCode){
         urlRepository.incrementClickCount(shortCode);
    }

    public List<UrlMapping> getAllUrls() {
        return urlRepository.findAll();
    }

    public List<Map<String, Object>> getTop10MostShortenedUrls() {
        Pageable pageable = PageRequest.of(0, 10);  // Create a Pageable object
        List<Object[]> results = urlRepository.findTop10MostShortenedUrls(pageable);

        List<Map<String, Object>> topUrls = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> data = new HashMap<>();
            data.put("originalUrl", row[0]);
            data.put("shortenedCount", row[1]);
            topUrls.add(data);
        }

        return topUrls;
    }

    public List<Map<String, Object>> getTop10MostClickedUrls() {
        Pageable pageable = PageRequest.of(0, 10);  // Get only 10 results
        List<Object[]> results = urlRepository.findTop10MostClickedUrls(pageable);

        return results.stream().map(row -> Map.of(
                "shortCode", row[0] != null ? row[0] : "N/A",
                "originalUrl", row[1] != null ? row[1] : "Unknown",
                "clickCount", row[2] != null ? row[2] : 0,
                "lastAccessedAt", row[3] != null ? row[3] : "Never Accessed"
        )).toList();
    }

    @Transactional
    public boolean deleteUrl(String shortCode, String apiKey, String password) {
        User user = userRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API Key"));

        UrlMapping urlMapping = urlRepository.findByShortCodeAndIsDeletedFalse(shortCode)
                .orElseThrow(() -> new RuntimeException("Short code not found"));

        // Check if the user owns the short code
        if (!urlMapping.getUserId().equals(user.getId())) {
            return false;
        }

        // If the URL has a password, validate it
        if (urlMapping.getPassword() != null) {
            if (password == null || password.isBlank()) {
                throw new RuntimeException("Password required for deletion");
            }

            if (!NUtil.verifyPassword(password, urlMapping.getPassword())) {
                throw new RuntimeException("Incorrect password");
            }
        }

        // Soft delete by marking as deleted
        urlMapping.setDeleted(true);
        urlRepository.save(urlMapping);

        return true;
    }

    public Optional<UrlMapping> getUrlMapping(String shortCode) {
        return urlRepository.findByShortCodeAndIsDeletedFalse(shortCode);
    }




}
