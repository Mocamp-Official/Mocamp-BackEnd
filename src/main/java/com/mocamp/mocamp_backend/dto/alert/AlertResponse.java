package com.mocamp.mocamp_backend.dto.alert;

import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private WebsocketMessageType type;
    private int minutesLeft;
}
