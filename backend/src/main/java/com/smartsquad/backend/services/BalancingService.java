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
     * Splits players into balanced teams.
     * Guaranteed to prioritize equal team sizes.
     */
    public List<Team> balanceTeams(List<PlayerEntity> players, int numberOfTeams) {
        if (players == null || players.isEmpty() || numberOfTeams < 2) return new ArrayList<>();

        // Calculate maximum allowed players per team based on total count
        int maxPlayersPerTeam = (int) Math.ceil((double) players.size() / numberOfTeams);

        //Sort by skill (Greedy best practice)
        List<PlayerEntity> sortedPlayers = new ArrayList<>(players);
        sortedPlayers.sort(Comparator.comparingInt(PlayerEntity::getSkillLevel)
                .thenComparingInt(PlayerEntity::getSecondarySkill)
                .reversed());

        //Init teams
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < numberOfTeams; i++) teams.add(new Team());

        Map<String, Integer> forcedTeamMap = new HashMap<>();
        Set<String> assignedNames = new HashSet<>();

        //Main Loop
        for (PlayerEntity player : sortedPlayers) {
            String pName = player.getName().toLowerCase();
            if (assignedNames.contains(pName)) continue;

            // Pre-calculate the size of the social group (Recursive chain)
            int chainSize = countChainSize(player, assignedNames, players, new HashSet<>());

            if (chainSize > maxPlayersPerTeam) {
                throw new IllegalArgumentException("Constraint Error: Group containing " + player.getName() +
                        " has " + chainSize + " players, which exceeds the team limit of " + maxPlayersPerTeam);
            }

            int targetIdx;
            if (forcedTeamMap.containsKey(pName)) {
                targetIdx = forcedTeamMap.get(pName);
                // rival safety check
                if (teams.get(targetIdx).getPlayers().size() + chainSize > maxPlayersPerTeam) {
                    throw new IllegalArgumentException("Impossible Balance: Rival constraints force a team to exceed size limit.");
                }
            } else {
                // Find the best team: Smallest Size FIRST, then Weakest Skill
                targetIdx = findBestTeamIndex(teams);
            }

            assignPlayerRecursive(player, targetIdx, teams, assignedNames, forcedTeamMap, players, numberOfTeams);
        }

        return teams;
    }

    /**
     * Recursively counts the size of a "Must-Be-With" chain to validate capacity.
     */
    private int countChainSize(PlayerEntity player, Set<String> assignedNames, List<PlayerEntity> allPlayers, Set<String> visited) {
        String pName = player.getName().toLowerCase();
        if (assignedNames.contains(pName) || visited.contains(pName)) return 0;

        visited.add(pName);
        int totalSize = 1;

        String partnerName = player.getHasToBeWith();
        if (partnerName != null) {
            Optional<PlayerEntity> partnerOpt = allPlayers.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(partnerName))
                    .findFirst();
            if (partnerOpt.isPresent()) {
                totalSize += countChainSize(partnerOpt.get(), assignedNames, allPlayers, visited);
            }
        }
        return totalSize;
    }

    /**
     * Recursively assigns a player and all linked partners to the same team.
     */
    private void assignPlayerRecursive(PlayerEntity player, int teamIdx, List<Team> teams,
                                       Set<String> assignedNames, Map<String, Integer> forcedMap,
                                       List<PlayerEntity> allPlayers, int totalTeams) {

        String pName = player.getName().toLowerCase();
        if (assignedNames.contains(pName)) return;

        //Assign current
        teams.get(teamIdx).addPlayer(player);
        assignedNames.add(pName);

        //Handle Rival (Cannot-Be-With) - Flag them for different teams later
        String rivalName = player.getCannotBeWith();
        if (rivalName != null) {
            String cleanRival = rivalName.toLowerCase();
            if (!forcedMap.containsKey(cleanRival)) {
                int rivalTeamIdx = findAlternativeTeamIndex(teamIdx, totalTeams, teams);
                forcedMap.put(cleanRival, rivalTeamIdx);
            }
        }

        // 3. Handle Partner (Has-To-Be-With) - RECURSIVE CHAIN
        String partnerName = player.getHasToBeWith();
        if (partnerName != null) {
            Optional<PlayerEntity> partnerOpt = allPlayers.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(partnerName))
                    .findFirst();
            if (partnerOpt.isPresent()) {
                assignPlayerRecursive(partnerOpt.get(), teamIdx, teams, assignedNames, forcedMap, allPlayers, totalTeams);
            }
        }
    }

    /**
     * Finds the best team in a single pass.
     * Priority 1: Lower Size (Player Count).
     * Priority 2: Lower Total Skill.
     */
    private int findBestTeamIndex(List<Team> teams) {
        int index = 0;
        for (int i = 1; i < teams.size(); i++) {
            Team current = teams.get(i);
            Team best = teams.get(index);

            if (current.getPlayers().size() < best.getPlayers().size() ||
                    (current.getPlayers().size() == best.getPlayers().size() && current.getTotalSkill() < best.getTotalSkill())) {
                index = i;
            }
        }
        return index;
    }

    /**
     * Finds an alternative team (for rivals) while respecting size parity.
     */
    private int findAlternativeTeamIndex(int forbiddenIdx, int totalTeams, List<Team> teams) {
        int index = -1;
        for (int i = 0; i < totalTeams; i++) {
            if (i == forbiddenIdx) continue;

            if (index == -1) {
                index = i;
            } else {
                Team current = teams.get(i);
                Team best = teams.get(index);

                if (current.getPlayers().size() < best.getPlayers().size() ||
                        (current.getPlayers().size() == best.getPlayers().size() && current.getTotalSkill() < best.getTotalSkill())) {
                    index = i;
                }
            }
        }
        return index;
    }
}