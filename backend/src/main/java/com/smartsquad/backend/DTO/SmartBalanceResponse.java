package com.smartsquad.backend.DTO;

import com.smartsquad.backend.services.BalancingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmartBalanceResponse {

    private String status;
    private List<BalancingService.Team> teams;
    private String reason;
    private String message;
    private List<String> conflictingPlayers;
    private boolean fallback;
    private List<Map<String, String>> appliedConstraints;

    public static SmartBalanceResponse success(List<BalancingService.Team> teams, boolean fallback,
                                                List<Map<String, String>> appliedConstraints) {
        SmartBalanceResponse r = new SmartBalanceResponse();
        r.setStatus("success");
        r.setTeams(teams);
        r.setFallback(fallback);
        r.setAppliedConstraints(appliedConstraints);
        return r;
    }

    public static SmartBalanceResponse conflict(String reason, String message, List<String> conflictingPlayers,
                                                 List<Map<String, String>> appliedConstraints) {
        SmartBalanceResponse r = new SmartBalanceResponse();
        r.setStatus("error");
        r.setReason(reason);
        r.setMessage(message);
        r.setConflictingPlayers(conflictingPlayers);
        r.setAppliedConstraints(appliedConstraints);
        return r;
    }
}
