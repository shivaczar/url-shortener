package org.bitly;

import org.bitly.entity.UrlMapping;
import org.bitly.repository.UrlRepository;
import org.bitly.service.RedisCacheService;
import org.bitly.service.UrlShortenerService;
import org.junit.jupiter.api.Assertions;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private UrlRepository urlRepository;

    @Test
    void testCachingBehavior() {
        String shortCode = "wUL6ha";
        String originalUrl = "https://example.com";

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setShortCode(shortCode);
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setUserId(1L);

        when(redisCacheService.getCachedUrl(shortCode)).thenReturn(null);
        when(urlRepository.findByShortCodeAndIsDeletedFalse(shortCode)).thenReturn(Optional.of(urlMapping));
        doNothing().when(redisCacheService).cacheUrl(shortCode, originalUrl, 3600);

        Optional<UrlMapping> result1 = urlShortenerService.getUrlMapping(shortCode);
        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(originalUrl, result1.get().getOriginalUrl());

        when(redisCacheService.getCachedUrl(shortCode)).thenReturn(originalUrl);

        Optional<UrlMapping> result2 = urlShortenerService.getUrlMapping(shortCode);
        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals(originalUrl, result2.get().getOriginalUrl());

        verify(urlRepository, times(1)).findByShortCodeAndIsDeletedFalse(shortCode);
    }
}

