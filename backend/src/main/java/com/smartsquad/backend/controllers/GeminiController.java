package com.smartsquad.backend.controllers;

import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.services.BalancingService;
import com.smartsquad.backend.services.GeminiService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * AIController
 * Entry point for "Smart" requests.
 * This controller uses Gemini AI to understand natural language and then
 * triggers the balancing logic.
 */
@AllArgsConstructor
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class GeminiController {


    private final GeminiService geminiService;
    private final BalancingService balancingService;

    /**
     * The Smart Balance Endpoint:
     * 1. Receives a natural language prompt (e.g., "Messi is here but Ronaldo is out").
     * 2. Uses Gemini AI to extract the list of attending player names.
     * 3. Fetches player data from the database.
     * 4. Runs the balancing algorithm on the found players.
     *
     * @param userPrompt The raw text from the user.
     * @param teams The number of teams to split into (default is 2).
     * @return A balanced list of Teams.
     */
    @PostMapping("/balance")
    public List<BalancingService.Team> smartBalance(
            @RequestBody String userPrompt,
            @RequestParam(defaultValue = "2") int teams) {


        List<String> attendingNames = geminiService.extractPlayerNames(userPrompt);

        if (attendingNames.isEmpty()) {
            throw new IllegalArgumentException("AI could not identify any participating players in your request.");
        }

        List<PlayerEntity> players = balancingService.findAllByNameIn(attendingNames);

        if (players.isEmpty()) {
            throw new IllegalArgumentException("Players found by AI (" + attendingNames + ") do not exist in the database.");
        }

        return balancingService.balanceTeams(players, teams);
    }
}
