package com.mocamp.mocamp_backend.dto.resolution;


import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionUpdateResponse {

    private WebsocketMessageType type;
    private String resolution;
}
