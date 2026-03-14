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
        Set<String> assignedNames = new HashSet<>();

        // 3. Main Loop
        for (PlayerEntity player : sortedPlayers) {
            String pName = player.getName().toLowerCase();
            if (assignedNames.contains(pName)) continue;

            int targetIdx = forcedTeamMap.getOrDefault(pName, findWeakestTeamIndex(teams));

            // Recursive assign player and all their "Must-With" chain
            assignPlayerRecursive(player, targetIdx, teams, assignedNames, forcedTeamMap, players, numberOfTeams);
        }
        return teams;
    }

    /**
     * Recursively assigns a player and all linked "must-be-with" partners to the same team.
     */
    private void assignPlayerRecursive(PlayerEntity player, int teamIdx, List<Team> teams,
                                       Set<String> assignedNames, Map<String, Integer> forcedMap,
                                       List<PlayerEntity> allPlayers, int totalTeams) {

        String pName = player.getName().toLowerCase();
        if (assignedNames.contains(pName)) return;

        // 1. Assign current player
        teams.get(teamIdx).addPlayer(player);
        assignedNames.add(pName);

        // 2. Handle Rival (Cannot-Be-With) - Flag them for a different team later
        String rivalName = player.getCannotBeWith();
        if (rivalName != null && !forcedMap.containsKey(rivalName.toLowerCase())) {
            int rivalTeamIdx = findAlternativeTeamIndex(teamIdx, totalTeams, teams);
            forcedMap.put(rivalName.toLowerCase(), rivalTeamIdx);
        }

        // 3. Handle Partner (Has-To-Be-With) - RECURSIVE CHAIN
        String partnerName = player.getHasToBeWith();
        if (partnerName != null) {
            Optional<PlayerEntity> partnerOpt = allPlayers.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(partnerName))
                    .findFirst();

            // If the partner is in this match, pull them into the same team immediately
            if (partnerOpt.isPresent()) {
                assignPlayerRecursive(partnerOpt.get(), teamIdx, teams, assignedNames, forcedMap, allPlayers, totalTeams);
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