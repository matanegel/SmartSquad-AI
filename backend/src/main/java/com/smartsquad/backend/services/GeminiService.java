package com.smartsquad.backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsquad.backend.models.PlayerEntity;
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

    private final PlayerService playerService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private static final String MODEL_NAME = "gemini-2.5-flash-lite";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1/models/";




    public String classifyIntent(String userMessage) {
        String prompt = "You are a router for a football squad management app. " +
                "Classify the user's message into exactly ONE of these intents:\n" +
                "- create_player  (user provides a specific player name and wants to ADD them. " +
                "Example: 'Create Messi with skill 5', 'Add Ronaldo')\n" +
                "- balance_teams  (user provides specific player names and wants to SHUFFLE/BALANCE them. " +
                "Example: 'Balance Messi and Ronaldo', 'Shuffle all players')\n" +
                "- list_players   (user wants to SEE all players in the database. " +
                "Example: 'Show all players', 'Who is registered?')\n" +
                "- general_chat   (user is asking a QUESTION, greeting, asking HOW to do something, " +
                "asking for help, or having a conversation. " +
                "Example: 'How to create a player?', 'What can you do?', 'Help', 'Hello')\n\n" +
                "IMPORTANT: If the user is ASKING HOW to do something (question), classify as general_chat. " +
                "Only classify as create_player or balance_teams if the user provides actual player names " +
                "and wants to perform the action NOW.\n\n" +
                "Return ONLY the intent keyword, nothing else.\n\n" +
                "User message: " + userMessage;

        String result = callGeminiWithRetry(prompt).trim().toLowerCase();

        if (List.of("create_player", "balance_teams", "list_players", "general_chat").contains(result)) {
            return result;
        }
        return "general_chat";
    }

    public String chat(String userMessage) {
        String prompt = "You are a friendly AI assistant for SmartSquad AI — a football squad management app. " +
                "Your capabilities:\n" +
                "1. CREATE PLAYER — user can say things like 'Create Messi with skill 5' or " +
                "'Add Ronaldo, he's a pro and must be with Benzema' and you'll create the player in the database.\n" +
                "2. BALANCE TEAMS — user can say 'Balance all players into 3 teams' or " +
                "'Shuffle Messi, Ronaldo, Neymar' and you'll split them into fair teams shown on the field.\n" +
                "3. LIST PLAYERS — user can say 'Show all players' or 'Who is in the database?' to see everyone.\n\n" +
                "Skill levels go from 1 (beginner) to 5 (pro). Players can have a partner (must be on same team) " +
                "or a rival (must be on different teams).\n\n" +
                "Answer the user's question naturally and helpfully. Keep it short (2-3 sentences max). " +
                "If they seem confused, give examples of what they can type.\n\n" +
                "User: " + userMessage;

        return callGeminiWithRetry(prompt);
    }

    public List<PlayerEntity> getPlayersFromPrompt(String userPrompt) {
        String lowerPrompt = userPrompt.toLowerCase();

        // Handle "all players" shortcut
        if (lowerPrompt.contains("all") || lowerPrompt.contains("everyone") || lowerPrompt.contains("כולם")) {
            return playerService.getAllPlayers();
        }

        // Use AI to extract names
        List<String> names = extractPlayerNames(userPrompt);
        if (names.isEmpty())  throw new IllegalArgumentException("AI could not identify any participating players in your request.");

        return playerService.getPlayersByNames(names);
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

        String resultText = callGeminiWithRetry(combinedPrompt);

        if (resultText == null || resultText.isEmpty()) return List.of();

        return Arrays.stream(resultText.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String callGeminiWithRetry(String fullPrompt) {
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
        return "";
    }

    private String performApiCall(String fullPrompt) {
        String url = BASE_URL + MODEL_NAME + ":generateContent?key=" + apiKey;

        // Structured payload for Gemini API
        // temperature for how much randomness in the output (low for more accurate and not creative)
        // maxOutputTokens for how many tokens to generate (higher for more longer responses)
        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", fullPrompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 200
                )
        );

        Map<String, Object> response = restTemplate.postForObject(url, payload, Map.class);

        try {
            // Parsing the complex Gemini response structure
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
            return parts.get(0).get("text").trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response");
        }
    }

    /**
     * Extracts player data from natural language and converts it into a PlayerEntity.
     * * @param userPrompt The free-text input (e.g., "Create Kaka with skill 5...")
     * @return A PlayerEntity object populated with extracted data.
     */
    public PlayerEntity parsePlayerFromText(String userPrompt) {
        String systemInstruction = """
                You are a data extraction assistant for a football management system.
                Your goal is to extract player details from user text and return them in STRICT JSON format.
                
                Fields to extract:
                - name: The player's name (string).
                - skillLevel: The primary skill level (integer, 1-5). Default to 3 if not mentioned.
                - secondarySkill: The secondary level or tie-breaker (integer). Default to 0.
                - hasToBeWith: Name of a friend/partner (string or null).
                - cannotBeWith: Name of a rival/enemy (string or null).
                
                Rules:
                - Return ONLY the JSON object. No preamble, no markdown formatting (like ```json), no explanations.
                - If a field is missing, use defaults or null.
                """;

        String fullPrompt = "System Instruction: " + systemInstruction + "\n\nUser Input: " + userPrompt;

        String aiResponse = callGeminiWithRetry(fullPrompt);

        try {
            // Clean AI response in case it wraps it in markdown blocks
            String jsonString = aiResponse.replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(jsonString, PlayerEntity.class);
        } catch (Exception e) {
            throw new RuntimeException("AI returned invalid player data: " + aiResponse);
        }
    }

}