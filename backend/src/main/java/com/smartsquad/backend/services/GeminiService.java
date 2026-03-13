package com.smartsquad.backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/**
 * GeminiService
 * Handles communication with Google Gemini API to extract entities from text.
 */
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final RestTemplate restTemplate;

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    private static final String MODEL_NAME = "gemini-2.5-flash-preview-09-2025";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=";

    /**
     * Calls Gemini AI to extract player names from a free-text prompt.
     * The method parses the complex Gemini API response by selecting the first
     * candidate's content and extracting the text from its parts.
     *
     * @param userInput Free-text containing names (e.g., "Messi and Ronaldo arrived").
     * @return A list of cleaned player names found in the text.
     */
    public List<String> extractPlayerNames(String userInput) {
        String systemPrompt = "You are an expert football match coordinator. " +
                "Analyze the provided text and extract ONLY the names of players who ARE attending or participating in the match. " +
                "If the text mentions a player is NOT coming, is injured, or absent, do NOT include them in the list. " +
                "Return the results as a comma-separated list of names. If no attending players are found, return an empty string. " +
                "Example: 'Messi and Neymar are here but Ronaldo isn't' -> Output: Messi, Neymar";

        return callGeminiWithRetry(userInput, systemPrompt);
    }

    private List<String> callGeminiWithRetry(String userQuery, String systemPrompt) {
        int maxRetries = 5;
        long delay = 1000; // Starting with 1s

        for (int i = 0; i < maxRetries; i++) {
            try {
                return performApiCall(userQuery, systemPrompt);
            } catch (Exception e) {
                if (i == maxRetries - 1) throw new RuntimeException("AI Service is currently unavailable. Please try again later.");
                try {
                    Thread.sleep(delay);
                    delay *= 2; // Exponential backoff: 1s, 2s, 4s, 8s, 16s
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return List.of();
    }

    private List<String> performApiCall(String userQuery, String systemPrompt) {
        String url = BASE_URL + apiKey;

        // Structured payload for Gemini API
        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", userQuery)))
                ),
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                )
        );

        Map<String, Object> response = restTemplate.postForObject(url, payload, Map.class);

        try {
            // Parsing the complex Gemini response structure
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
            String resultText = parts.get(0).get("text").trim();

            if (resultText.isEmpty()) return List.of();

            return Arrays.stream(resultText.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response");
        }
    }
}