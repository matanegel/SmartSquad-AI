package com.smartsquad.backend.services;

import com.smartsquad.backend.DTO.PlayerResponse;
import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Player Service
 */
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    public List<PlayerEntity> getAllPlayers() {
        return playerRepository.findAll();
    }

    public PlayerResponse savePlayer(PlayerEntity player) {
        if (playerRepository.findByName(player.getName()) != null) {
            throw new IllegalArgumentException("Player " + player.getName() + " already exists");
        }
        PlayerEntity savedPlayer =  playerRepository.save(player);
        return new PlayerResponse(
                savedPlayer.getName() + " was created successfully",
                savedPlayer
        );

    }

    public void deletePlayer(String name) {
        if (playerRepository.findByName(name) == null) {
            throw new IllegalArgumentException("Player " + name + " not found");
        }
        playerRepository.deleteByName(name);
    }

    public void deleteAllPlayers() {
        playerRepository.deleteAll();
    }
}