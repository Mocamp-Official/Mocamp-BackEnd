package com.mocamp.mocamp_backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebsocketErrorMessage {

    private Long userId;
    private String message;

}
