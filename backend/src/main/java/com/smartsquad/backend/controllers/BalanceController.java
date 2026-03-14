package com.smartsquad.backend.controllers;

import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.services.BalancingService;
import com.smartsquad.backend.services.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/balance")
@CrossOrigin(origins = "*")
public class BalanceController {

    private final BalancingService balancingService;
    private final PlayerService playerService;

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
}
