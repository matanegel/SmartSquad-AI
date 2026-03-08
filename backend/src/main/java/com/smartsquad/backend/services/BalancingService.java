package com.smartsquad.backend.services;

import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.repositories.PlayerRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * BalancingService
 * Implements the logic to split players into N balanced teams.
 */
@Service
@RequiredArgsConstructor
public class BalancingService {

        private final PlayerRepository playerRepository;

    /**
     * Internal class to represent a Team during the calculation
     */
    @Getter
    public static class Team {
        private final List<PlayerEntity> players = new ArrayList<>();
        private int totalSkill = 0;

        public void addPlayer(PlayerEntity player) {
            players.add(player);
            totalSkill += player.getSkillLevel();
        }

    }


    public List<PlayerEntity> findAllByNameIn(List<String> names) {
        return playerRepository.findAllByNameIn(names);
    }

    /**
     * Splits a list of players into N balanced teams.
     */
    public List<Team> balanceTeams(List<PlayerEntity> players, int numberOfTeams) {
        if (players == null || players.isEmpty()) return new ArrayList<>();

        // 1. Sort players by Skill Level (Primary) and Secondary Skill (Tie-breaker)
        List<PlayerEntity> sortedPlayers = players.stream()
                .sorted(Comparator.comparingInt(PlayerEntity::getSkillLevel)
                        .thenComparingInt(PlayerEntity::getSecondarySkill)
                        .reversed())
                .toList();

        // 2. Initialize N empty teams
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < numberOfTeams; i++) {
            teams.add(new Team());
        }

        // 3. Greedy distribution
        for (PlayerEntity player : sortedPlayers) {
            // Find the team with the minimum total skill score
            Team weakestTeam = teams.stream()
                    .min(Comparator.comparingInt(Team::getTotalSkill))
                    .orElse(teams.getFirst());

            weakestTeam.addPlayer(player);
        }

        return teams;
    }
}