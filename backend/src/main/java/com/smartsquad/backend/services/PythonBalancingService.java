package com.smartsquad.backend.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsquad.backend.DTO.SmartBalanceResponse;
import com.smartsquad.backend.models.PlayerEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonBalancingService {

    private final BalancingService balancingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final long TIMEOUT_SECONDS = 30;

    public SmartBalanceResponse balance(List<PlayerEntity> players, int numTeams,
                                         List<Map<String, String>> constraints) {
        try {
            String result = executePython(players, numTeams, constraints);
            Map<String, Object> parsed = objectMapper.readValue(result, new TypeReference<>() {});

            String status = (String) parsed.get("status");

            if ("success".equals(status)) {
                List<Map<String, Object>> rawTeams = (List<Map<String, Object>>) parsed.get("teams");
                List<BalancingService.Team> teams = convertToTeams(rawTeams);
                return SmartBalanceResponse.success(teams, false);
            } else {
                String reason = (String) parsed.getOrDefault("reason", "unknown");
                String message = (String) parsed.getOrDefault("message", "Unknown error from solver");
                List<String> conflicting = parsed.containsKey("conflicting_players")
                        ? (List<String>) parsed.get("conflicting_players")
                        : List.of();
                return SmartBalanceResponse.conflict(reason, message, conflicting);
            }
        } catch (Exception e) {
            log.warn("Python balancer failed, falling back to Java greedy: {}", e.getMessage());
            return fallback(players, numTeams);
        }
    }

    private String executePython(List<PlayerEntity> players, int numTeams,
                                  List<Map<String, String>> constraints) throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("numTeams", numTeams);

        List<Map<String, Object>> playerList = players.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", p.getName());
            m.put("skillLevel", p.getSkillLevel());
            m.put("secondarySkill", p.getSecondarySkill());
            return m;
        }).collect(Collectors.toList());
        input.put("players", playerList);
        input.put("constraints", constraints);

        String jsonInput = objectMapper.writeValueAsString(input);

        Path scriptPath = resolveScriptPath();

        ProcessBuilder pb = new ProcessBuilder("python", scriptPath.toString());
        pb.redirectErrorStream(false);

        Process process = pb.start();

        try (OutputStream os = process.getOutputStream()) {
            os.write(jsonInput.getBytes());
            os.flush();
        }

        String stdout;
        String stderr;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            stdout = reader.lines().collect(Collectors.joining("\n"));
            stderr = errReader.lines().collect(Collectors.joining("\n"));
        }

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python script timed out after " + TIMEOUT_SECONDS + "s");
        }

        if (process.exitValue() != 0 && stdout.isEmpty()) {
            throw new RuntimeException("Python script error: " + stderr);
        }

        return stdout.trim();
    }

    private Path resolveScriptPath() {
        Path relative = Paths.get("scripts", "balance.py");
        if (relative.toFile().exists()) return relative;

        Path fromBackend = Paths.get("backend", "scripts", "balance.py");
        if (fromBackend.toFile().exists()) return fromBackend;

        Path absolute = Paths.get(System.getProperty("user.dir"), "scripts", "balance.py");
        if (absolute.toFile().exists()) return absolute;

        throw new RuntimeException("Cannot find balance.py script");
    }

    private List<BalancingService.Team> convertToTeams(List<Map<String, Object>> rawTeams) {
        List<BalancingService.Team> teams = new ArrayList<>();
        for (Map<String, Object> rt : rawTeams) {
            BalancingService.Team team = new BalancingService.Team();
            List<Map<String, Object>> rawPlayers = (List<Map<String, Object>>) rt.get("players");
            for (Map<String, Object> rp : rawPlayers) {
                PlayerEntity pe = PlayerEntity.builder()
                        .name((String) rp.get("name"))
                        .skillLevel(((Number) rp.get("skillLevel")).intValue())
                        .secondarySkill(rp.get("secondarySkill") != null
                                ? ((Number) rp.get("secondarySkill")).intValue() : 0)
                        .build();
                team.addPlayer(pe);
            }
            teams.add(team);
        }
        return teams;
    }

    private SmartBalanceResponse fallback(List<PlayerEntity> players, int numTeams) {
        try {
            List<BalancingService.Team> teams = balancingService.balanceTeams(players, numTeams);
            return SmartBalanceResponse.success(teams, true);
        } catch (Exception e) {
            return SmartBalanceResponse.conflict("fallback_error", e.getMessage(), List.of());
        }
    }
}
