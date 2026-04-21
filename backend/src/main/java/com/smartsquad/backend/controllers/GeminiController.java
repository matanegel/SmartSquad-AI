package com.smartsquad.backend.controllers;

import com.smartsquad.backend.DTO.ChatRequest;
import com.smartsquad.backend.DTO.ChatResponse;
import com.smartsquad.backend.DTO.PlayerResponse;
import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.services.BalancingService;
import com.smartsquad.backend.services.GeminiService;
import com.smartsquad.backend.services.PlayerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@AllArgsConstructor
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class GeminiController {

    private final GeminiService geminiService;
    private final BalancingService balancingService;
    private final PlayerService playerService;

    @PostMapping("/balance")
    public List<BalancingService.Team> smartBalance(
            @RequestBody String userPrompt,
            @RequestParam(defaultValue = "2") int teams) {

        List<PlayerEntity> players = geminiService.getPlayersFromPrompt(userPrompt);
        return balancingService.balanceTeams(players, teams);
    }

    @PostMapping("/create")
    public PlayerResponse createPlayer(@RequestBody String userPrompt) {
        return playerService.savePlayer(geminiService.parsePlayerFromText(userPrompt));
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String intent = geminiService.classifyIntent(request.getMessage());

        try {
            return switch (intent) {
                case "create_player" -> handleCreatePlayer(request.getMessage());
                case "balance_teams" -> handleBalanceTeams(request.getMessage(), request.getNumTeams());
                case "list_players" -> handleListPlayers();
                default -> ChatResponse.of(
                        "I'm not sure what you want. Try something like:\n" +
                        "• \"Create Messi with skill 5\"\n" +
                        "• \"Balance Messi, Ronaldo, Neymar into teams\"\n" +
                        "• \"Show all players\"",
                        "unknown", null);
            };
        } catch (Exception e) {
            return ChatResponse.error("Something went wrong: " + e.getMessage());
        }
    }

    private ChatResponse handleCreatePlayer(String message) {
        PlayerEntity player = geminiService.parsePlayerFromText(message);
        PlayerResponse saved = playerService.savePlayer(player);
        String reply = "Created " + saved.getPlayer().getName() +
                " (skill " + saved.getPlayer().getSkillLevel() + ")";
        if (saved.getPlayer().getHasToBeWith() != null)
            reply += ", partner: " + saved.getPlayer().getHasToBeWith();
        if (saved.getPlayer().getCannotBeWith() != null)
            reply += ", rival: " + saved.getPlayer().getCannotBeWith();
        return ChatResponse.of(reply, "create_player", saved.getPlayer());
    }

    private ChatResponse handleBalanceTeams(String message, int numTeams) {
        List<PlayerEntity> players = geminiService.getPlayersFromPrompt(message);
        List<BalancingService.Team> teams = balancingService.balanceTeams(players, numTeams);
        String playerNames = players.stream()
                .map(PlayerEntity::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
        String reply = "Balanced " + players.size() + " players (" + playerNames +
                ") into " + numTeams + " teams!";
        return ChatResponse.of(reply, "balance_teams", teams);
    }

    private ChatResponse handleListPlayers() {
        List<PlayerEntity> all = playerService.getAllPlayers();
        if (all.isEmpty()) {
            return ChatResponse.of("No players in the database yet. Try creating some!", "list_players", all);
        }
        String names = all.stream()
                .map(p -> p.getName() + " (" + p.getSkillLevel() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return ChatResponse.of(all.size() + " players: " + names, "list_players", all);
    }
}
