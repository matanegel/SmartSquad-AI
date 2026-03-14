package com.smartsquad.backend.balance;

import com.smartsquad.backend.models.PlayerEntity;
import com.smartsquad.backend.services.BalancingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class BalancingServiceTest {

    @InjectMocks
    private BalancingService balancingService;

    @Test
    public void testBalancingParity() {
        // Create 4 players: 2 high skill, 2 low skill
        PlayerEntity p1 = PlayerEntity.builder().name("Pro1").skillLevel(5).secondarySkill(5).build();
        PlayerEntity p2 = PlayerEntity.builder().name("Pro2").skillLevel(5).secondarySkill(5).build();
        PlayerEntity p3 = PlayerEntity.builder().name("Noob1").skillLevel(1).secondarySkill(1).build();
        PlayerEntity p4 = PlayerEntity.builder().name("Noob2").skillLevel(1).secondarySkill(1).build();

        List<PlayerEntity> players = Arrays.asList(p1, p2, p3, p4);

        // Split into 2 teams
        List<BalancingService.Team> teams = balancingService.balanceTeams(players, 2);

        assertEquals(2, teams.size());
        // Each team should have one Pro and one Noob (Sum 6)
        assertEquals(6, teams.get(0).getTotalSkill());
        assertEquals(6, teams.get(1).getTotalSkill());
    }

    @Test
    public void testEmptyList() {
        List<BalancingService.Team> teams = balancingService.balanceTeams(null, 3);
        assertTrue(teams.isEmpty());
    }
}