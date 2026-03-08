package com.smartsquad.backend.controllers;

import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.services.BalancingService;
import com.smartsquad.backend.services.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Player Controller
 * Exposes the /api/players endpoint for the frontend.
 */
@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Needed for future React integration
public class PlayerController {

    private final PlayerService playerService;
    private final BalancingService balancingService;

    /**
     * Get all players from the database.
     * Access via: GET http://localhost:8080/api/players
     */
    @GetMapping
    public List<PlayerEntity> getAllPlayers() {
        return playerService.getAllPlayers();
    }


    @PostMapping
    public List<PlayerEntity> getSpecificPlayers(@RequestBody List<String> names) {
        return balancingService.findAllByNameIn(names);
    }

    /**
     * Add a new player.
     * Access via: POST http://localhost:8080/api/players
     */
    @PostMapping
    public PlayerEntity createPlayer(@RequestBody PlayerEntity player) {
        return playerService.savePlayer(player);
    }

    /**
     * Remove a player by ID.
     * Access via: DELETE http://localhost:8080/api/players?name=messi
     */
    @DeleteMapping
    public void deletePlayer(@RequestParam String name) {
        playerService.deletePlayer(name);
    }


}
