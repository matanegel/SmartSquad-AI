package com.smartsquad.backend.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private String intent;
    private Object data;

    public static ChatResponse of(String reply, String intent, Object data) {
        return new ChatResponse(reply, intent, data);
    }

    public static ChatResponse error(String reply) {
        return new ChatResponse(reply, "error", null);
    }
}
