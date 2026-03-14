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

        // map: Player Name -> Forced Team Index
        Map<String, Integer> forcedTeamMap = new HashMap<>();

        // 3. Main Loop
        for (PlayerEntity player : sortedPlayers) {
            String pName = player.getName().toLowerCase();

            // Determine target team index
            int targetTeamIndex;
            if (forcedTeamMap.containsKey(pName)) {
                targetTeamIndex = forcedTeamMap.get(pName);
            } else {
                // Regular Greedy: Pick the weakest team
                targetTeamIndex = findWeakestTeamIndex(teams);
            }

            // Assign the player
            Team targetTeam = teams.get(targetTeamIndex);
            targetTeam.addPlayer(player);

            // 4. Update "Flags" for constraints
            updateConstraintFlags(player, targetTeamIndex, numberOfTeams, teams, forcedTeamMap);
        }

        return teams;
    }

    /**
     * Updates the map with forced assignments based on the current player's constraints.
     */
    private void updateConstraintFlags(PlayerEntity player, int currentTeamIdx, int totalTeams, List<Team> teams, Map<String, Integer> forcedMap) {
        String mustWith = player.getHasToBeWith();
        String cannotWith = player.getCannotBeWith();

        // If player has a MUST partner -> they are forced to the same team
        if (mustWith != null) {
            forcedMap.put(mustWith.toLowerCase(), currentTeamIdx);
        }

        // If player has a CANNOT rival -> they are forced to a DIFFERENT team
        if (cannotWith != null) {
            String rivalName = cannotWith.toLowerCase();
            // If the rival wasn't already forced elsewhere, find the next best team for them
            if (!forcedMap.containsKey(rivalName)) {
                int rivalTeamIdx = findAlternativeTeamIndex(currentTeamIdx, totalTeams, teams);
                forcedMap.put(rivalName, rivalTeamIdx);
            }
        }
    }

    private int findWeakestTeamIndex(List<Team> teams) {
        int minSkill = Integer.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).getTotalSkill() < minSkill) {
                minSkill = teams.get(i).getTotalSkill();
                index = i;
            }
        }
        return index;
    }

    /**
     * Finds the best team for a rival (someone who cannot be in currentTeamIdx).
     * Usually picks the current weakest team that is NOT the forbidden one.
     */
    private int findAlternativeTeamIndex(int forbiddenIdx, int totalTeams, List<Team> teams) {
        int minSkill = Integer.MAX_VALUE;
        int index = (forbiddenIdx + 1) % totalTeams; // Default fallback

        for (int i = 0; i < totalTeams; i++) {
            if (i == forbiddenIdx) continue;
            if (teams.get(i).getTotalSkill() < minSkill) {
                minSkill = teams.get(i).getTotalSkill();
                index = i;
            }
        }
        return index;
    }
}