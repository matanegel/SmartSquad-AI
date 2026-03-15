package com.smartsquad.backend.services;

import com.smartsquad.backend.DTO.PlayerResponse;
import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public PlayerEntity getPlayerByName(String name) {
        return playerRepository.findByName(name);
    }

    public List<PlayerEntity> getPlayersByNames(List<String> names) {
        return playerRepository.findAllByNameIn(names);
    }

    /**
     * Saves a player and synchronizes constraints with existing players in the DB.
     * Ensures that relationships are always bidirectional.
     */
    @Transactional
    public PlayerResponse savePlayer(PlayerEntity player) {
        String cleanName = player.getName().toLowerCase();
        player.setName(cleanName);

        //Check if player already exists
        if (playerRepository.findByName(cleanName) != null) {
            throw new IllegalArgumentException("Player " + cleanName + " already exists.");
        }

        //Normalize constraint inputs
        player.setHasToBeWith(player.getHasToBeWith() != null ? player.getHasToBeWith().toLowerCase() : null);
        player.setCannotBeWith(player.getCannotBeWith() != null ? player.getCannotBeWith().toLowerCase() : null);

        //Sync existing "claims" from the database to this new player
        // This prevents overwriting a pre-existing relationship established by another player.
        syncIncomingConstraints(player);

        //Final logical validation (Self-checks)
        validateConstraints(player);

        //Save the primary entity
        PlayerEntity saved = playerRepository.save(player);

        //Apply Reciprocity: Update the partner/rival to point back to this player
        // If this fails (e.g., target is already taken), the transaction rolls back.
        applyReciprocity(saved);

        return new PlayerResponse(
                saved.getName() + " was created successfully",
                saved);
    }

    /**
     * Checks if other players in the DB already point to this player.
     * If they do, this player must point back to them.
     */
    private void syncIncomingConstraints(PlayerEntity player) {
        String name = player.getName();

        // PARTNER CHECK: Does anyone else say "I must be with Messi"?
        List<PlayerEntity> existingFans = playerRepository.findByHasToBeWith(name);
        if (!existingFans.isEmpty()) {
            String existingPartner = existingFans.get(0).getName();

            // If the user provided a different partner, we have a conflict.
            if (player.getHasToBeWith() != null && !player.getHasToBeWith().equalsIgnoreCase(existingPartner)) {
                throw new IllegalArgumentException("Conflict: " + name + " is already 'claimed' by " + existingPartner +
                        " as a partner. You cannot link them to " + player.getHasToBeWith() + ".");
            }
            // Auto-sync the relationship
            player.setHasToBeWith(existingPartner);
        }

        // RIVAL CHECK: Does anyone else say "I cannot be with Messi"?
        List<PlayerEntity> existingEnemies = playerRepository.findByCannotBeWith(name);
        if (!existingEnemies.isEmpty()) {
            String existingRival = existingEnemies.get(0).getName();

            if (player.getCannotBeWith() != null && !player.getCannotBeWith().equalsIgnoreCase(existingRival)) {
                throw new IllegalArgumentException("Conflict: " + name + " is already 'claimed' by " + existingRival +
                        " as a rival. You cannot link them to " + player.getCannotBeWith() + ".");
            }
            player.setCannotBeWith(existingRival);
        }
    }

    /**
     * Ensures bidirectional links. If Messi -> Neymar, then Neymar -> Messi.
     * Throws an error if the target is already linked to someone else.
     */
    private void applyReciprocity(PlayerEntity player) {
        // Handle Partner Reciprocity
        if (player.getHasToBeWith() != null) {
            String partnerName = player.getHasToBeWith();
            PlayerEntity partner = playerRepository.findByName(partnerName);

            if (partner != null) {
                // If partner already points to someone else, we have a broken link
                if (partner.getHasToBeWith() != null && !partner.getHasToBeWith().equalsIgnoreCase(player.getName())) {
                    throw new IllegalArgumentException("Conflict: " + partnerName + " is already linked to " + partner.getHasToBeWith() + ".");
                }
                playerRepository.updateHasToBeWithByName(partnerName, player.getName());
            }
        }

        // Handle Rival Reciprocity
        if (player.getCannotBeWith() != null) {
            String rivalName = player.getCannotBeWith();
            PlayerEntity rival = playerRepository.findByName(rivalName);

            if (rival != null) {
                if (rival.getCannotBeWith() != null && !rival.getCannotBeWith().equalsIgnoreCase(player.getName())) {
                    throw new IllegalArgumentException("Conflict: " + rivalName + " is already forced away from " + rival.getCannotBeWith() + ".");
                }
                playerRepository.updateCannotBeWithByName(rivalName, player.getName());
            }
        }
    }

   /* public PlayerResponse savePlayer(PlayerEntity player) {
        player.setName(player.getName().toLowerCase());
        player.setCannotBeWith(player.getCannotBeWith() != null ? player.getCannotBeWith().toLowerCase() : null );
        player.setHasToBeWith(player.getHasToBeWith() != null ? player.getHasToBeWith().toLowerCase() : null );

        if (Optional.ofNullable(playerRepository.findByName(player.getName())).isPresent()){
            throw new IllegalArgumentException("Player " + player.getName() + " already exists");
        }
        if (player.getSkillLevel() < 0 || player.getSkillLevel() > 5) {
            throw new IllegalArgumentException("Skill level must be between 0 and 5");
        }

        validateConstraints(player);

        PlayerEntity savedPlayer =  playerRepository.save(player);
        return new PlayerResponse(
                savedPlayer.getName() + " was created successfully",
                savedPlayer
        );

    }*/

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

    public void updateSkillLevel(PlayerEntity player, int newSkillLevel) {
        if (newSkillLevel < 0 || newSkillLevel > 5) {
            throw new IllegalArgumentException("Skill level must be between 0 and 5");
        }
        int rowsChanged = playerRepository.updateSkillLevelByName(player.getName(), newSkillLevel);
        if (rowsChanged == 0) {
            throw new IllegalArgumentException("Player " + player.getName() + " not found");
        }
    }
}