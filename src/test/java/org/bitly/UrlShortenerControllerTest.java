package org.bitly;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.bitly.controller.UrlShortenerController;
import org.bitly.entity.UrlMapping;
import org.bitly.entity.User;
import org.bitly.repository.UrlRepository;
import org.bitly.repository.UserRepository;
import org.bitly.service.UrlShortenerService;
import org.bitly.util.NUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UrlShortenerControllerTest {

    @InjectMocks
    private UrlShortenerController urlShortenerController;  // Inject Controller Manually

    @Mock
    private UrlShortenerService urlShortenerService;  // Mock Service

    @Mock
    private UserRepository userRepository;

    @Mock
    private UrlRepository urlRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(urlShortenerController).build();
    }

    @Test
    void testShortenUrl_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(urlShortenerController).build();

        Map<String, String> request = Map.of(
                "url", "https://example.com",
                "customCode", "myShort",
                "expiryDate", "2025-12-31T23:59:59",
                "password", "securePass"
        );

        when(urlShortenerService.shortenUrl(anyString(), anyString(), any(), any(), any())).thenReturn("myShort");

        mockMvc.perform(post("/api/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "test-api-key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("myShort"));
    }

    @Test
    void testShortenUrl_MissingUrl() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(urlShortenerController).build();

        Map<String, String> request = Map.of("customCode", "myShort");

        mockMvc.perform(post("/api/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "test-api-key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("URL is required"));
    }

    @Test
    void testShortenUrl_ServiceThrowsException() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(urlShortenerController).build();

        Map<String, String> request = Map.of("url", "https://example.com");

        when(urlShortenerService.shortenUrl(anyString(), anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("Custom short code is already taken"));

        mockMvc.perform(post("/api/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "test-api-key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Custom short code is already taken"));
    }


    @Test
    public void testShortenUrls_Success() throws Exception {
        List<Map<String, String>> urlRequests = List.of(
                Map.of("originalUrl", "https://example.com"),
                Map.of("originalUrl", "https://another.com")
        );

        List<Map<String, String>> shortenedUrls = List.of(
                Map.of("originalUrl", "https://example.com", "shortCode", "abc123"),
                Map.of("originalUrl", "https://another.com", "shortCode", "xyz789")
        );

        // Mock HttpServletRequest
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

        // Mock the service method to match both arguments properly
        Mockito.when(urlShortenerService.shortenUrls(Mockito.anyList(), Mockito.any(HttpServletRequest.class)))
                .thenReturn(shortenedUrls);

        mockMvc.perform(post("/api/urls/shorten/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "test-api-key")
                        .content(new ObjectMapper().writeValueAsString(Map.of("urls", urlRequests))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].shortCode").value("abc123"))
                .andExpect(jsonPath("$[1].shortCode").value("xyz789"));

        // Verify that the service was called with the correct parameters
        Mockito.verify(urlShortenerService).shortenUrls(Mockito.anyList(), Mockito.any(HttpServletRequest.class));
    }


    @Test
    public void testUpdateExpiry_Success() throws Exception {
        String shortCode = "abc123";
        String apiKey = "test-api-key";
        String expiryDate = "2025-12-31T23:59:59";

        // No exception means the update is successful
        Mockito.doNothing().when(urlShortenerService)
                .updateExpiry(Mockito.eq(shortCode), Mockito.eq(apiKey), Mockito.any(LocalDateTime.class));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/urls/shorten/{shortCode}/expiry", shortCode)
                        .param("apiKey", apiKey)
                        .param("expiryDate", expiryDate))
                .andExpect(status().isOk())
                .andExpect(content().string("Expiry date updated successfully."));
    }

    @Test
    public void testUpdateExpiry_Forbidden() throws Exception {
        String shortCode = "abc123";
        String apiKey = "test-api-key";
        String expiryDate = "2025-12-31T23:59:59";

        Mockito.doThrow(new RuntimeException("Unauthorized to update expiry date"))
                .when(urlShortenerService)
                .updateExpiry(Mockito.eq(shortCode), Mockito.eq(apiKey), Mockito.any(LocalDateTime.class));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/urls/shorten/{shortCode}/expiry", shortCode)
                        .param("apiKey", apiKey)
                        .param("expiryDate", expiryDate))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Unauthorized to update expiry date"));
    }


    @Test
    public void testRedirectToOriginalUrl_Success() throws Exception {
        String shortCode = "abc123";
        String originalUrl = "https://example.com";

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setShortCode(shortCode);
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setPassword(null); // No password protection

        Mockito.when(urlShortenerService.getUrlMapping(shortCode)).thenReturn(Optional.of(urlMapping));
        Mockito.doNothing().when(urlShortenerService).incrementClick(shortCode);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/urls/redirect")
                        .param("code", shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    public void testRedirectToOriginalUrl_NotFound() throws Exception {
        String shortCode = "invalidCode";

        Mockito.when(urlShortenerService.getUrlMapping(shortCode)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/urls/redirect")
                        .param("code", shortCode))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Short code not found or expired"));
    }

    @Test
    public void testRedirectToOriginalUrl_PasswordRequired() throws Exception {
        String shortCode = "protectedCode";
        String originalUrl = "https://example.com";
        String hashedPassword = NUtil.hashPassword("secure123");

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setShortCode(shortCode);
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setPassword(hashedPassword);

        Mockito.when(urlShortenerService.getUrlMapping(shortCode)).thenReturn(Optional.of(urlMapping));

        // No password provided
        mockMvc.perform(MockMvcRequestBuilders.get("/api/urls/redirect")
                        .param("code", shortCode))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Password required for this short code"));
    }

    @Test
    public void testRedirectToOriginalUrl_InvalidPassword() throws Exception {
        String shortCode = "protectedCode";
        String originalUrl = "https://example.com";
        String hashedPassword = NUtil.hashPassword("secure123");

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setShortCode(shortCode);
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setPassword(hashedPassword);

        Mockito.when(urlShortenerService.getUrlMapping(shortCode)).thenReturn(Optional.of(urlMapping));

        // Provide incorrect password
        mockMvc.perform(MockMvcRequestBuilders.get("/api/urls/redirect")
                        .param("code", shortCode)
                        .param("password", "wrongPassword"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid password"));
    }

    @Test
    public void testRedirectToOriginalUrl_CorrectPassword() throws Exception {
        String shortCode = "protectedCode";
        String originalUrl = "https://example.com";
        String correctPassword = "secure123";
        String hashedPassword = NUtil.hashPassword(correctPassword);

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setShortCode(shortCode);
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setPassword(hashedPassword);

        Mockito.when(urlShortenerService.getUrlMapping(shortCode)).thenReturn(Optional.of(urlMapping));
        Mockito.doNothing().when(urlShortenerService).incrementClick(shortCode);

        // Provide correct password
        mockMvc.perform(MockMvcRequestBuilders.get("/api/urls/redirect")
                        .param("code", shortCode)
                        .param("password", correctPassword))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    public void testDeleteShortCode_Success() throws Exception {
        String shortCode = "abc123";
        String apiKey = "testApiKey";

        Mockito.when(urlShortenerService.deleteUrl(shortCode, apiKey, null)).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/urls/delete/{shortCode}", shortCode)
                        .header("X-API-KEY", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Short code deleted successfully"));
    }

    @Test
    public void testDeleteShortCode_WithPassword_Success() throws Exception {
        String shortCode = "protectedCode";
        String apiKey = "testApiKey";
        String password = "correctPassword";

        Mockito.when(urlShortenerService.deleteUrl(shortCode, apiKey, password)).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/urls/delete/{shortCode}", shortCode)
                        .header("X-API-KEY", apiKey)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Short code deleted successfully"));
    }

    @Test
    public void testDeleteShortCode_InvalidPassword() throws Exception {
        String shortCode = "protectedCode";
        String apiKey = "testApiKey";
        String wrongPassword = "wrongPassword";

        Mockito.when(urlShortenerService.deleteUrl(shortCode, apiKey, wrongPassword)).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/urls/delete/{shortCode}", shortCode)
                        .header("X-API-KEY", apiKey)
                        .param("password", wrongPassword))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Unauthorized or incorrect password for deletion"));
    }

    @Test
    public void testDeleteShortCode_Unauthorized() throws Exception {
        String shortCode = "abc123";
        String apiKey = "invalidApiKey";

        Mockito.when(urlShortenerService.deleteUrl(shortCode, apiKey, null)).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/urls/delete/{shortCode}", shortCode)
                        .header("X-API-KEY", apiKey))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Unauthorized or incorrect password for deletion"));
    }

    @Test
    public void testGetAllUrls_Success() throws Exception {
        String apiKey = "testApiKey";
        User user = new User();
        user.setId(1L);

        List<UrlMapping> urlMappings = List.of(
                new UrlMapping("abc123", "http://example.com", user.getId(), null, "pwd"),
                new UrlMapping("xyz789", "http://test.com", user.getId(), LocalDateTime.now().plusDays(10), "hashedPassword")
        );

        Mockito.when(userRepository.findByApiKey(apiKey)).thenReturn(Optional.of(user));
        Mockito.when(urlRepository.findByUserIdAndIsDeletedFalse(user.getId())).thenReturn(urlMappings);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/urls/user")
                        .header("X-API-KEY", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].shortCode").value("abc123"))
                .andExpect(jsonPath("$[0].originalUrl").value("http://example.com"))
                .andExpect(jsonPath("$[0].expiryDate").value("Never"))
                .andExpect(jsonPath("$[0].clicks").value(0))
                .andExpect(jsonPath("$[0].passwordProtected").value(true))
                .andExpect(jsonPath("$[1].shortCode").value("xyz789"))
                .andExpect(jsonPath("$[1].originalUrl").value("http://test.com"))
                .andExpect(jsonPath("$[1].passwordProtected").value(true));
    }

    @Test
    public void testGetAllUrls_InvalidApiKey() throws Exception {
        String invalidApiKey = "invalidApiKey";

        Mockito.when(userRepository.findByApiKey(invalidApiKey)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/urls/user")
                        .header("X-API-KEY", invalidApiKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid API Key"));
    }

    @Test
    public void testGetAllUrls_NoUrlsFound() throws Exception {
        String apiKey = "testApiKey";
        User user = new User();
        user.setId(1L);

        Mockito.when(userRepository.findByApiKey(apiKey)).thenReturn(Optional.of(user));
        Mockito.when(urlRepository.findByUserIdAndIsDeletedFalse(user.getId())).thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/urls/user")
                        .header("X-API-KEY", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0)); // Expecting an empty list
    }


}

