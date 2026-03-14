package com.smartsquad.backend.DTO;

import com.smartsquad.backend.models.PlayerEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class PlayerResponse {

    String message;
    PlayerEntity player;
}
