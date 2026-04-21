package com.smartsquad.backend.controllers;

import com.smartsquad.backend.DTO.SmartBalanceRequest;
import com.smartsquad.backend.DTO.SmartBalanceResponse;
import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.services.BalancingService;
import com.smartsquad.backend.services.PlayerService;
import com.smartsquad.backend.services.PythonBalancingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/balance")
@CrossOrigin(origins = "*")
public class BalanceController {

    private final BalancingService balancingService;
    private final PlayerService playerService;
    private final PythonBalancingService pythonBalancingService;

    @PostMapping
    public List<BalancingService.Team> balanceTeams(
            @RequestBody List<String> playerNames,
            @RequestParam(defaultValue = "3") int numTeams)  {
        if(numTeams < 2) {
            throw new IllegalArgumentException("Number of teams must be at least 2");
        }

        List<PlayerEntity> players = balancingService.findAllByNameIn(playerNames);
        return balancingService.balanceTeams(players, numTeams);
    }

    @PostMapping("/smart")
    public SmartBalanceResponse smartBalance(@RequestBody SmartBalanceRequest request) {
        if (request.getNumTeams() < 2) {
            throw new IllegalArgumentException("Number of teams must be at least 2");
        }

        List<PlayerEntity> players = balancingService.findAllByNameIn(request.getPlayerNames());
        List<Map<String, String>> constraints = buildConstraints(players, request.getExcludedConstraints());

        return pythonBalancingService.balance(players, request.getNumTeams(), constraints);
    }

    private List<Map<String, String>> buildConstraints(List<PlayerEntity> players,
                                                        List<Map<String, String>> excluded) {
        Set<String> excludeKeys = excluded.stream()
                .map(e -> constraintKey(e.get("playerA"), e.get("playerB"), e.get("type")))
                .collect(Collectors.toSet());

        List<Map<String, String>> constraints = new ArrayList<>();

        for (PlayerEntity p : players) {
            if (p.getHasToBeWith() != null && !p.getHasToBeWith().isEmpty()) {
                String key = constraintKey(p.getName(), p.getHasToBeWith(), "must_be_with");
                if (!excludeKeys.contains(key)) {
                    constraints.add(Map.of(
                            "type", "must_be_with",
                            "playerA", p.getName().toLowerCase(),
                            "playerB", p.getHasToBeWith().toLowerCase()
                    ));
                }
            }
            if (p.getCannotBeWith() != null && !p.getCannotBeWith().isEmpty()) {
                String key = constraintKey(p.getName(), p.getCannotBeWith(), "cannot_be_with");
                if (!excludeKeys.contains(key)) {
                    constraints.add(Map.of(
                            "type", "cannot_be_with",
                            "playerA", p.getName().toLowerCase(),
                            "playerB", p.getCannotBeWith().toLowerCase()
                    ));
                }
            }
        }

        return constraints;
    }

    private String constraintKey(String a, String b, String type) {
        String low1 = a.toLowerCase();
        String low2 = b.toLowerCase();
        String first = low1.compareTo(low2) < 0 ? low1 : low2;
        String second = low1.compareTo(low2) < 0 ? low2 : low1;
        return type + ":" + first + ":" + second;
    }
}
