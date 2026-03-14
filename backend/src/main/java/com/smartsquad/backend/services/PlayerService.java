package com.smartsquad.backend.services;

import com.smartsquad.backend.DTO.PlayerResponse;
import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
        player.setName(player.getName().toLowerCase());
        player.setCannotBeWith(player.getCannotBeWith() != null ? player.getCannotBeWith().toLowerCase() : null );
        player.setHasToBeWith(player.getHasToBeWith() != null ? player.getHasToBeWith().toLowerCase() : null );

        if (Optional.ofNullable(playerRepository.findByName(player.getName())).isPresent()){
            throw new IllegalArgumentException("Player " + player.getName() + " already exists");
        }

        validateConstraints(player);

        PlayerEntity savedPlayer =  playerRepository.save(player);
        return new PlayerResponse(
                savedPlayer.getName() + " was created successfully",
                savedPlayer
        );

    }

    public void deletePlayer(String name) {
        name = name.toLowerCase();
        if (playerRepository.findByName(name) == null) {
            throw new IllegalArgumentException("Player " + name + " not found");
        }
        playerRepository.deleteByName(name);
    }

    public void deleteAllPlayers() {
        playerRepository.deleteAll();
    }

    /**
     * Validates complex logical constraints to prevent impossible team assignments.
     * Now handles transitive chains (e.g., A must be with B, B must be with C, so A cannot avoid C).
     */
    private void validateConstraints(PlayerEntity player) {
        String name = player.getName();
        String mustWith = player.getHasToBeWith();
        String cannotWith = player.getCannotBeWith();

        //Direct Self-Contradiction
        if (mustWith != null && cannotWith != null && mustWith.equalsIgnoreCase(cannotWith)) {
            throw new IllegalArgumentException("Conflict: " + name + " cannot be forced to stay with and stay away from the same person (" + mustWith + ").");
        }

        //Transitive "Must-With" Logic
        if (mustWith != null) {
            if (mustWith.equalsIgnoreCase(name)) throw new IllegalArgumentException("You cannot be your own partner.");

            Optional.ofNullable(playerRepository.findByName(mustWith)).ifPresent(partner -> {
                // Check if my partner already avoids me
                if (name.equalsIgnoreCase(partner.getCannotBeWith())) {
                    throw new IllegalArgumentException("Conflict: " + mustWith + " already has a rule to stay away from you.");
                }

                // Transitive: If I must be with B, and B must be with C, I am linked to C
                String partnersPartner = partner.getHasToBeWith();
                if (partnersPartner != null) {
                    if (cannotWith != null && cannotWith.equalsIgnoreCase(partnersPartner)) {
                        throw new IllegalArgumentException("Conflict: You must be with " + mustWith + ", who is linked to " + partnersPartner + " (who you avoid).");
                    }
                }
            });
        }

        //Transitive "Cannot-With" Logic (Handles the Messi-Neymar-Ronaldo-Messi case)
        if (cannotWith != null) {
            if (cannotWith.equalsIgnoreCase(name)) throw new IllegalArgumentException("You cannot avoid yourself.");

            Optional.ofNullable(playerRepository.findByName(cannotWith)).ifPresent(rival -> {
                // Direct Conflict: My rival is forced to be with me
                if (name.equalsIgnoreCase(rival.getHasToBeWith())) {
                    throw new IllegalArgumentException("Conflict: " + cannotWith + " is already forced to be with you.");
                }

                // Transitive Conflict: My rival must be with someone (X).
                // If X is someone I must be with, OR if X is forced to be with me... it's a conflict.
                String rivalsPartnerName = rival.getHasToBeWith();
                if (rivalsPartnerName != null) {
                    // Check if rival's partner is someone I'm forced to be with
                    if (mustWith != null && mustWith.equalsIgnoreCase(rivalsPartnerName)) {
                        throw new IllegalArgumentException("Conflict: You avoid " + cannotWith + ", but they must be with " + rivalsPartnerName + " (who you must be with).");
                    }

                    // Check if rival's partner is forced to be with me (The Messi-Neymar-Ronaldo loop)
                    Optional.ofNullable(playerRepository.findByName(rivalsPartnerName)).ifPresent(rivalsPartner -> {
                        if (name.equalsIgnoreCase(rivalsPartner.getHasToBeWith())) {
                            throw new IllegalArgumentException("Transitive Conflict: You avoid " + cannotWith + ", but they are linked to " + rivalsPartnerName + " who is forced to be with you.");
                        }
                    });
                }
            });
        }
    }

}