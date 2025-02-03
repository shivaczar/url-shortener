package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String shortCode;

    private String randomUrl;

    @BeforeEach
    public void setUp() throws Exception {
        // Prepare the JSON body for the /shorten POST request
        String jsonBody = "{\"url\": \"https://example.com\"}";
        randomUrl = "https://example.com/" + UUID.randomUUID().toString();

        // POST request to /shorten to generate short code for the long URL
        String response = mockMvc.perform(post("/api/shorten")
                        .contentType("application/json")
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").exists()) // Expect shortCode field in response
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the shortCode from the JSON response
        shortCode = response.split("\"shortCode\":\"")[1].split("\"")[0]; // Extract shortCode (WyLDA7)
    }

    @Test
    public void testRedirect() throws Exception {
        // Call /redirect API with the short code and expect redirection to the original URL
        mockMvc.perform(get("/api/redirect")
                        .param("code", shortCode))
                .andExpect(status().is3xxRedirection())  // Expect a redirection status
                .andExpect(redirectedUrl("https://example.com"));  // Check if redirection is to the original URL
    }

    @Test
    public void testShortenUrlTwice() throws Exception {
        // First request: Send a random URL to shorten
        String shortCodeResponse1 = mockMvc.perform(MockMvcRequestBuilders.post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"" + randomUrl + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the short code from the response
        String shortCode1 = extractShortCode(shortCodeResponse1);

        // Second request: Send the same random URL to shorten again
        String shortCodeResponse2 = mockMvc.perform(MockMvcRequestBuilders.post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"" + randomUrl + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the short code from the second response
        String shortCode2 = extractShortCode(shortCodeResponse2);

        // Assert that the short code from the second request is the same as the first request
        assert shortCode1.equals(shortCode2) : "Short codes should be the same for the same URL!";
    }

    private String extractShortCode(String response) {
        // This method assumes that the response body contains a JSON with a field 'shortCode'
        // Example response: {"shortCode": "abc123"}
        return response.substring(response.indexOf("\"shortCode\":\"") + 13, response.indexOf("\"}", response.indexOf("\"shortCode\":\"")));
    }


    @Test
    public void testDeleteShortCode() throws Exception {
        // 1. Create a short URL
        String requestBody = "{\"url\": \"https://example.com\"}";
        String response = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract the shortCode from response
        String shortCode = response.replace("{\"shortCode\":\"", "").replace("\"}", "");

        // 2. Delete the short URL
        mockMvc.perform(delete("/api/delete/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(content().string("Short code deleted successfully."));

        // 3. Try to delete again (should return 404)
        mockMvc.perform(delete("/api/delete/" + shortCode))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Short code not found."));
    }
}
