package com.smartsquad.backend.services;

import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.repositories.PlayerRepository;
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

    private final PlayerRepository playerRepository;
    private final RestTemplate restTemplate;

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private static final String MODEL_NAME = "gemini-2.5-flash-lite";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1/models/";




    public List<PlayerEntity> getPlayersFromPrompt(String userPrompt) {
        String lowerPrompt = userPrompt.toLowerCase();

        // Handle "all players" shortcut
        if (lowerPrompt.contains("all") || lowerPrompt.contains("everyone") || lowerPrompt.contains("כולם")) {
            return playerRepository.findAll();
        }

        // Use AI to extract names
        List<String> names = extractPlayerNames(userPrompt);
        if (names.isEmpty())  throw new IllegalArgumentException("AI could not identify any participating players in your request.");

        return playerRepository.findAllByNameIn(names);
    }


    /**
     * Calls Gemini AI to extract player names from a free-text prompt.
     * The method parses the complex Gemini API response by selecting the first
     * candidate's content and extracting the text from its parts.
     *
     * @param userInput Free-text containing names (e.g., "Messi and Ronaldo arrived").
     * @return A list of cleaned player names found in the text.
     */
    public List<String> extractPlayerNames(String userInput) {
        // We put the instructions directly in the prompt to avoid JSON field compatibility issues (systemInstruction)
        String combinedPrompt = "Act as a football coordinator. Extract the names of players who are attending " +
                "from the text below. Return ONLY a comma-separated list of names. " +
                "Example: Messi, Ronaldo. If no names are found, return an empty string.\n\n" +
                "Input Text: " + userInput;

        return callGeminiWithRetry(combinedPrompt);
    }

    private List<String> callGeminiWithRetry(String fullPrompt) {
        int maxRetries = 5;
        long delay = 1000; // Starting with 1s

        for (int i = 0; i < maxRetries; i++) {
            try {
                return performApiCall(fullPrompt);
            } catch (Exception e) {
                if (i == maxRetries - 1) throw new RuntimeException("Gemini API error " + e.getMessage());
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

    private List<String> performApiCall(String fullPrompt) {
        String url = BASE_URL + MODEL_NAME + ":generateContent?key=" + apiKey;

        // Structured payload for Gemini API
        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", fullPrompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 50
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