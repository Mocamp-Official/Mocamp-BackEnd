package com.mocamp.mocamp_backend.dto.delegation;

import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DelegationUpdateResponse {
    private WebsocketMessageType type;
    private Long previousAdminId;
    private Long newAdminId;
}
