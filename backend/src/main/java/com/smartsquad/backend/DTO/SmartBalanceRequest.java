package com.smartsquad.backend.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmartBalanceRequest {

    private List<String> playerNames;
    private int numTeams = 3;
    private List<Map<String, String>> excludedConstraints = new ArrayList<>();
    private List<Map<String, String>> additionalConstraints = new ArrayList<>();
}
