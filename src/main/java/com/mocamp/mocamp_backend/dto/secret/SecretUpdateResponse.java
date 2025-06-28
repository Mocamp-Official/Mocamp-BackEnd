package com.mocamp.mocamp_backend.dto.secret;

import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SecretUpdateResponse {
    private WebsocketMessageType type;
    private Long userId;
    private Boolean isSecret;
}
