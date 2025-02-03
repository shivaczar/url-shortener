package org.example.service;

import org.example.entity.UrlMapping;
import org.example.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
public class UrlShortenerService {

    @Autowired
    private UrlRepository urlRepository;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;

    // Method to shorten the URL
    public String shortenUrl(String originalUrl) {
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
            UrlMapping urlMapping = new UrlMapping(shortCode, originalUrl);
            urlRepository.save(urlMapping);
            return shortCode;
        }
    }

    // Method to get the original URL by short code
    public Optional<String> getOriginalUrl(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
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
}
